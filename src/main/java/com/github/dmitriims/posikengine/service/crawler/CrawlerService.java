package com.github.dmitriims.posikengine.service.crawler;

import com.github.dmitriims.posikengine.exceptions.IndexingStatusException;
import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.service.CommonContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class CrawlerService extends RecursiveAction {

    private volatile CommonContext commonContext;
    private volatile CrawlerContext context;
    protected String link;

    private final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    public CrawlerService(String link, CrawlerContext context, CommonContext commonContext) {
        this.commonContext = commonContext;
        this.context = context;
        this.link = decodeLink(link); //TODO: посмотреть, можно ли делать проверку перед decodeLink
        context.getVisitedPages().add(link);
    }

    public CrawlerService(CrawlerContext context, CommonContext commonContext) {
        this.commonContext = commonContext;
        this.context = context;
        this.link = decodeLink(context.getSite().getUrl());
        context.getVisitedPages().add(link);

        log.info("started task for site " + context.getSite().getUrl());
    }


    @Override
    protected void compute() {
        try {
            if (!commonContext.isIndexing() || context.getThisPool().isShutdown() || context.getNumberOfPagesToCrawl().get() <= 0) {
                return;
            }

            if (commonContext.isIndexing() && isSiteIndexedOrInterrupted(context.getSite())) {
                if (context.isReindexOnePage()) {
                    log.info("indexing page " + link + " for site " + context.getSite().getUrl());
                    Connection.Response response = getResponseFromLink(link);
                    if(!response.contentType().startsWith("text")) {
                        throw new IndexingStatusException("Страницы с типом \"" + response.contentType() + "\" не участвуют в индексировании");
                    }
                    Page onePage = getPageFromResponse(response);
                    Document document = response.parse();
                    List<Lemma> lemmas = getAndRankAllLemmas(document);
                    commonContext.getDatabaseService().saveOrUpdatePage(context.getSite(), onePage, lemmas, commonContext);
                    return;

                } else {
                    log.info("reindexing site " + context.getSite().getUrl());
                    synchronized (commonContext.getDatabaseService()) {
                        if (commonContext.isIndexing()) {
                            context.setSite(commonContext.getDatabaseService().setSiteStatusToIndexing(context.getSite()));
                        }
                    }
                }
            } else if (!commonContext.isIndexing()) {
                return;
            }

            Thread.sleep(context.getDelayGenerator().ints(500, 5001).findFirst().getAsInt());

            Set<String> filteredLinks = processOnePage(link);
            if (commonContext.isIndexing() && filteredLinks.size() != 0) {
                invokeAll(filteredLinks.stream().map(link -> new CrawlerService(link, context, commonContext)).toList());
            }
        } catch (IOException ioe) {
            log.error(ioe.toString());
        } catch (InterruptedException ie) {
            log.warn(ie.toString());
        }
    }

    boolean isSiteIndexedOrInterrupted(Site site) {
        return site.getStatus().equals(Status.INDEXED) ||
                (site.getStatus().equals(Status.FAILED) && site.getLastError().equals("Индексация прервана пользователем"));
    }

    Set<String> processOnePage(String url) throws IOException {
        Connection.Response response = getResponseFromLink(url);
        if (response == null || !response.contentType().startsWith("text")) {
            return new HashSet<>();
        }
        Page currentPage = getPageFromResponse(response);

        Document document = new Document(currentPage.getPath());
        List<Lemma> allLemmas = new ArrayList<>();

        if(currentPage.getCode() >= 200 && currentPage.getCode() < 400) {
            document = response.parse();
            allLemmas = getAndRankAllLemmas(document);
        }

        currentPage.setLemmasHashcode(calculateLemmasHash(allLemmas));

        if (commonContext.isIndexing() && context.getNumberOfPagesToCrawl().decrementAndGet() >= 0) {
            synchronized (commonContext.getDatabaseService()) {
                commonContext.getDatabaseService().saveOrUpdatePage(context.getSite(), currentPage, allLemmas, commonContext);
            }
            return filterLinks(
                    document.select("a[href]")
                            .stream()
                            .map(e -> {
                                String link = e.attr("abs:href");
                                return decodeLink(link);
                            }).toList()
            );
        }

        return new HashSet<>();
    }

    Connection.Response getResponseFromLink(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(commonContext.getUserAgent())
                    .referrer("http://www.google.com")
                    .timeout(60 * 1000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
        } catch (IOException ioe) {
            log.warn(ioe.getMessage());
            return null;
        }
    }

    Page getPageFromResponse(Connection.Response response) {
        int code = response.statusCode();
        String content = response.body();

        Page page = new Page();
        page.setSite(context.getSite());
        page.setPath(link.replaceFirst(context.getSite().getUrl(),
                link.equals(context.getSite().getUrl()) ? "/" : ""));
        page.setCode(code);
        page.setContent(content);
        return page;
    }

    List<Lemma> getAndRankAllLemmas(Document doc) throws IOException {
        List<Lemma> allLemmas = new ArrayList<>();
        for (Field f : context.getFields()) {
            String fieldText = doc.select(f.getSelector()).text();
            for (Map.Entry<String, Integer> lemmaCount : commonContext.getMorphologyService().getAndCountLemmasInString(fieldText).entrySet()) {
                Lemma tempLemma = new Lemma();
                tempLemma.setSite(context.getSite());
                tempLemma.setLemma(lemmaCount.getKey());
                tempLemma.setFrequency(lemmaCount.getValue());
                tempLemma.setRank(lemmaCount.getValue() * f.getWeight());

                int index = allLemmas.indexOf(tempLemma);
                if (index < 0) {
                    allLemmas.add(tempLemma);
                    continue;
                }
                Lemma toUpdate = allLemmas.get(index);
                toUpdate.setFrequency(toUpdate.getFrequency() + tempLemma.getFrequency());
                toUpdate.setRank(toUpdate.getRank() + tempLemma.getRank());
            }
        }
        return allLemmas;
    }

    int calculateLemmasHash(List<Lemma> lemmas) {
        int hashcode = 0;
        for (Lemma l : lemmas) {
            hashcode += l.getLemma().hashCode() * l.getFrequency();
        }
        return hashcode;
    }

    Set<String> filterLinks(List<String> links) {
        Set<String> filtered = new HashSet<>();
        for (String l : links) {
            if (wasVisited(l) || !l.startsWith(context.getSite().getUrl()) || containsForbiddenComponents(l) ||
                    !ifYearIsPresentIsItInAcceptableRange(link) || !context.getRobotsRules().isAllowed(l)) {
                context.getVisitedPages().add(l);
                continue;
            }
            filtered.add(l);
        }
        return filtered;
    }

    boolean wasVisited(String link) {
        String paired;
        if (link.endsWith("/")) {
            paired = link.substring(0, link.length() - 1);
        } else {
            paired = link.concat("/");
        }

        return context.getVisitedPages().contains(link) || context.getVisitedPages().contains(paired);
    }

    boolean containsForbiddenComponents(String link) {
        for (String component : commonContext.getFORBIDDEN_COMPONENTS()) {
            if (link.contains(component)) {
                return true;
            }
        }
        return false;
    }

    boolean ifYearIsPresentIsItInAcceptableRange(String link) {
        if (!link.contains("?")) {
            return true;
        }
        link = link.toLowerCase();
        int index = link.indexOf("year=");
        if (index < 0) {
            return true;
        }
        index += 5;
        if(index + 4 > link.length()) {
            return false;
        }
        try {
            int yearInLink = Integer.parseInt(link.substring(index, index + 4));
            int currentYear = LocalDate.now().getYear();
            if( yearInLink < currentYear - 10 || yearInLink > currentYear + 3) {
                return false;
            }
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

    String decodeLink(String link) {
        link = link.replaceAll("%(?![\\da-fA-F]{2})", "%25");
        link = link.replaceAll("\\+", "%2B");
        return URLDecoder.decode(link, StandardCharsets.UTF_8);
    }
}

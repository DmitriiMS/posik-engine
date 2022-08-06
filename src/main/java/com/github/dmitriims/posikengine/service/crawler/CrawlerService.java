package com.github.dmitriims.posikengine.service.crawler;

import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Page;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.service.CommonContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.RecursiveAction;


public class CrawlerService extends RecursiveAction {

    private final CommonContext commonContext;
    private final CrawlerContext context;
    protected String link;

    private final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    public CrawlerService(String link, CrawlerContext context, CommonContext commonContext) {
        this.commonContext = commonContext;
        this.context = context;
        this.link = link;
        context.getVisitedPages().add(link);
    }

    public CrawlerService(CrawlerContext context, CommonContext commonContext) {
        this.commonContext = commonContext;
        this.context = context;
        this.link = context.getSite().getUrl();

        log.info("started task for site " + context.getSite().getUrl());
    }

    @Override
    protected void compute() {
        try {
            if (context.getThisPool().isShutdown() || context.getNumberOfPagesToCrawl().get() < 0) {
                return;
            }

            if (context.getSite().getStatus().equals(Status.INDEXED)) {
                log.info("cleaning up db for site " + context.getSite().getUrl());
                synchronized (commonContext.getDatabaseService()) {
                    commonContext.getDatabaseService().deleteSiteInformation(context.getSite());
                    context.setSite(commonContext.getDatabaseService().setSiteStatusToIndexing(context.getSite()));
                }
            }

            Thread.sleep(context.getDelayGenerator().ints(500, 5000).findFirst().getAsInt());

            Connection.Response response = Jsoup.connect(link)
                    .userAgent(commonContext.getUserAgent())
                    .referrer("http://www.google.com")
                    .timeout(60 * 1000)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
            String type = response.contentType();
            if (!type.startsWith("text")) {
                return;
            }
            int code = response.statusCode();
            String content = response.body();

            Page currentPage = new Page();
            currentPage.setSite(context.getSite());
            currentPage.setPath(link.replaceFirst(context.getSite().getUrl(), "/"));
            currentPage.setCode(code);
            currentPage.setContent(content);

            Document document = response.parse();
            List<Lemma> allLemmas = getAndRankAllLemmas(document);

            if (context.getNumberOfPagesToCrawl().decrementAndGet() >= 0) {
                synchronized (commonContext.getDatabaseService()) {
                    commonContext.getDatabaseService().savePageToDataBase(context.getSite(), currentPage, allLemmas);

                }
            }

            Set<String> filteredLinks = filterLinks(
                    document.select("a[href]")
                            .stream()
                            .map(e -> {
                                String link = e.attr("abs:href");
                                link = link.replaceAll("%(?![\\da-fA-F]{2})", "%25");
                                link = link.replaceAll("\\+", "%2B");
                                return URLDecoder.decode(link, StandardCharsets.UTF_8);
                            }).toList()
            );
            if (filteredLinks.size() != 0) {
                invokeAll(filteredLinks.stream().map(link -> new CrawlerService(link, context, commonContext)).toList());
            }
        } catch (IOException ioe) {
            log.error(ioe.toString());
        } catch (InterruptedException ie) {
            log.warn(ie.toString());
        }
    }

    public List<Lemma> getAndRankAllLemmas(Document doc) throws IOException {
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

    public Set<String> filterLinks(List<String> links) {
        Set<String> filtered = new HashSet<>();
        for (String l : links) {
            if (context.getVisitedPages().contains(l) || !l.startsWith(context.getSite().getUrl()) || containsForbiddenComponents(l) ||
                    !context.getRobotsMatcher().singleAgentAllowedByRobots(commonContext.getUserAgent(), l)) {
                context.getVisitedPages().add(l);
                continue;
            }
            filtered.add(l);
        }
        return filtered;
    }

    private boolean containsForbiddenComponents(String link) {
        for (String component : commonContext.getFORBIDDEN_COMPONENTS()) {
            if (link.contains(component)) {
                return true;
            }
        }
        return false;
    }
}

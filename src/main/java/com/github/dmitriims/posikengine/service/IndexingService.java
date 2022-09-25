package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.dto.userprovaideddata.SiteUrlAndNameDTO;
import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import com.github.dmitriims.posikengine.exceptions.IndexingStatusException;
import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import com.github.dmitriims.posikengine.service.crawler.CrawlerService;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IndexingService {

    private UserProvidedData userProvidedData;
    private SimpleRobotRulesParser robotsParser;
    private CommonContext commonContext;

    private Map<Site, ForkJoinPool> sitePools;
    @Getter
    private Thread indexingMonitorTread;

    private final Logger log = LoggerFactory.getLogger(IndexingService.class);

    public IndexingService(UserProvidedData userProvidedData, SimpleRobotRulesParser robotsParser, CommonContext commonContext) {
        this.userProvidedData = userProvidedData;
        this.robotsParser = robotsParser;
        this.commonContext = commonContext;
    }

    Runnable indexingMonitor = () -> {
        try {
            while (true) {
                Thread.sleep(1000);
                if (sitePools.isEmpty()) {
                    commonContext.setIndexing(false);
                    break;
                }

                Iterator<Map.Entry<Site, ForkJoinPool>> it = sitePools.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Site, ForkJoinPool> pool = it.next();

                    if (pool.getValue().isShutdown()) {
                        log.info("ожидаю, пока рабочие завершат все текущие задачи для сайта " + pool.getKey().getUrl());
                        if (!pool.getValue().awaitTermination(10, TimeUnit.SECONDS)) {
                            log.warn("пул не остановился за заданный период ожидания, отпускаю фронт");
                        }
                        synchronized (commonContext.getDatabaseService()) {
                            commonContext.getDatabaseService().cleanSavedPagesCache();
                            commonContext.getDatabaseService().setSiteStatusToFailed(pool.getKey(), "Индексация прервана пользователем");
                        }
                        log.info("индексация прервана для сайта " + pool.getKey().getUrl());
                        it.remove();
                        continue;
                    }

                    if (pool.getValue().isQuiescent()) {
                        synchronized (commonContext.getDatabaseService()) {
                            boolean indexedAnything = commonContext.getDatabaseService().removeDeletedPagesForSite(pool.getKey().getId());
                            if(indexedAnything) {
                                commonContext.getDatabaseService().setSiteStatusToIndexed(pool.getKey());
                            } else {
                                commonContext.getDatabaseService().setSiteStatusToFailed(pool.getKey(), "Ничего не проиндексировано");
                            }
                        }
                        log.info("закончена индексация для сайта " + pool.getKey().getUrl());
                        pool.getValue().shutdownNow();
                        it.remove();
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("Indexing-monitor прерван во время ожидания!");
            throw new RuntimeException();
        }
    };

    public boolean isIndexing() {
        return commonContext.isIndexing();
    }

    public boolean isSiteIndexing(String siteUrl) {
        return isIndexing() && isSiteStatusEqualsIndexing(siteUrl);
    }

    private boolean isSiteStatusEqualsIndexing(String siteUrl) {
        return commonContext.getDatabaseService().getSiteByUrl(siteUrl).getStatus().equals(Status.INDEXING);
    }

    public void setIndexing(boolean flag) {
        commonContext.setIndexing(flag);
    }

    public IndexingStatusResponse startIndexing() throws IOException, IndexingStatusException {
        boolean wasNotIndexing = false;

        if (!commonContext.isIndexing()) {
            wasNotIndexing = true;
            commonContext.setIndexing(true);
            commonContext.resetIndexingMessage();
            sitePools = new ConcurrentHashMap<>();
        }

        List<Site> sites = commonContext.getDatabaseService().getAllSites();
        sites.removeIf(site -> site.getStatus().equals(Status.INDEXING));
        if(sites.size() == 0) {
            throw new IndexingStatusException("Индексация уже запущена");
        }
        List<Field> fields = commonContext.getDatabaseService().getAllFields();

        for (Site site : sites) {
            addSiteAndStartIndexing(site, Integer.MAX_VALUE, fields);
        }

        if (wasNotIndexing) {
            indexingMonitorTread = new Thread(indexingMonitor, "Indexing-monitor");
            indexingMonitorTread.start();
        }

        return new IndexingStatusResponse(true, null);
    }

    public IndexingStatusResponse indexSite(String siteUrl) throws IOException, IndexingStatusException {
        boolean wasNotIndexing = false;

        if (isSiteIndexing(siteUrl)) {
            throw new IndexingStatusException("Индексация для сайта " + siteUrl + " уже запущена");
        }
        if (!isIndexing()) {
            wasNotIndexing = true;
            commonContext.setIndexing(true);
            commonContext.resetIndexingMessage();
            sitePools = new ConcurrentHashMap<>();
        }
        Site site = commonContext.getDatabaseService().getSiteByUrl(siteUrl);
        List<Field> fields = commonContext.getDatabaseService().getAllFields();
        addSiteAndStartIndexing(site, Integer.MAX_VALUE, fields);
        if (wasNotIndexing) {
            indexingMonitorTread = new Thread(indexingMonitor, "Indexing-monitor");
            indexingMonitorTread.start();
        }
        return new IndexingStatusResponse(true, null);
    }

    public IndexingStatusResponse stopIndexing() throws IndexingStatusException {
        if (!commonContext.isIndexing()) {
            throw new IndexingStatusException("Индексация не запущена");
        }
        commonContext.setIndexing(false);
        commonContext.setIndexingMessage("Индексация прервана пользователем");

        log.info("индексация останавливается, рабочим отправлена команда остановиться");
        Iterator<Map.Entry<Site, ForkJoinPool>> it = sitePools.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Site, ForkJoinPool> pool = it.next();
            pool.getValue().shutdownNow();
        }

        try {
            indexingMonitorTread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new IndexingStatusResponse(true, null);
    }

    public IndexingStatusResponse indexOnePage(String url) throws IOException, IndexingStatusException {

        boolean wasNotIndexing = false;

        String mimeType = URLConnection.guessContentTypeFromName(url);

        if (mimeType != null && !mimeType.startsWith("text")) {
            throw new IndexingStatusException("Страницы с типом \"" + URLConnection.guessContentTypeFromName(url) + "\" не участвуют в индексировании");
        }

        sitePools = new ConcurrentHashMap<>();
        List<Field> fields = commonContext.getDatabaseService().getAllFields();
        List<String> userProvidedSitesUrls = userProvidedData.getSites().stream().map(SiteUrlAndNameDTO::getUrl).collect(Collectors.toList());

        String siteUrl = "";
        for (String userUrl : userProvidedSitesUrls) {
            if (url.startsWith(userUrl)) {
                siteUrl = userUrl;
            }
        }
        if (siteUrl.isBlank()) {
            throw new IndexingStatusException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        if (!isIndexing()) {
            wasNotIndexing = true;
            commonContext.setIndexing(true);
            commonContext.resetIndexingMessage();
            sitePools = new ConcurrentHashMap<>();
        }

        Site site = commonContext.getDatabaseService().getSiteByUrl(siteUrl);
        addOnePageAndIndex(site, url, fields);
        if (wasNotIndexing) {
            indexingMonitorTread = new Thread(indexingMonitor, "Indexing-Monitor");
            indexingMonitorTread.start();
        }
        return new IndexingStatusResponse(true, null);
    }

    public static byte[] getRobotsTxt(String site) throws IOException {
        byte[] robotsInBytes;
        URL robots = new URL(site + "/robots.txt");

        HttpURLConnection urlConnection = (HttpURLConnection) robots.openConnection();
        urlConnection.setRequestMethod("HEAD");
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return "User-agent: *\nAllow:".getBytes(StandardCharsets.UTF_8);
        }

        try (InputStream inStream = robots.openStream();
             ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n = 0;
            while ((n = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, n);
            }
            robotsInBytes = outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return robotsInBytes;
    }

    public String getTopLevelUrl(String url) {
        String[] splitSite = url.split("//|/");
        return splitSite[0] + "//" + splitSite[1];
    }

    public void addSiteAndStartIndexing(Site site, int limit, List<Field> fields) throws IOException {
        CrawlerContext currentContext = generateCrawlerContext(site, limit, fields);
        currentContext.setReindexOnePage(false);
        CrawlerService crawler = new CrawlerService(currentContext, commonContext);
        launchIndexing(currentContext, crawler);
        log.info("запущена индексация для сайта " + site.getUrl());
    }

    public void addOnePageAndIndex(Site site, String url, List<Field> fields) throws IOException {
        CrawlerContext currentContext = generateCrawlerContext(site, 1, fields);
        currentContext.setReindexOnePage(true);
        CrawlerService crawler = new CrawlerService(url, currentContext, commonContext);
        launchIndexing(currentContext, crawler);
        log.info("индексируется одна страница: " + url);
    }

    private void launchIndexing(CrawlerContext context, CrawlerService crawler) {
        sitePools.put(context.getSite(), context.getThisPool());
        sitePools.get(context.getSite()).execute(crawler);
    }

    public CrawlerContext generateCrawlerContext(Site site, int limit, List<Field> fields) throws IOException {
        String topLevelSite = getTopLevelUrl(site.getUrl());
        ForkJoinPool pool = new ForkJoinPool();
        BaseRobotRules robotRules = robotsParser.parseContent(topLevelSite + "/robots.txt", getRobotsTxt(topLevelSite), "text/plain", commonContext.getUserAgent());
        return new CrawlerContext(site, pool, limit, new HashSet<>(fields), robotRules);
    }

}

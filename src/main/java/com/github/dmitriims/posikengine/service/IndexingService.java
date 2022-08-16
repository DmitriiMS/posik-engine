package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.dto.userprovaideddata.SiteUrlAndNameDTO;
import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import com.github.dmitriims.posikengine.exceptions.IndexingStatusException;
import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import com.github.dmitriims.posikengine.service.crawler.CrawlerService;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IndexingService {

    @Resource
    private UserProvidedData userProvidedData;
    @Resource
    private SimpleRobotRulesParser robotsParser;
    @Resource
    private CommonContext commonContext;

    private Map<Site, ForkJoinPool> sitePools;
    @Getter
    private Thread indexingMonitorTread;

    private final Logger log = LoggerFactory.getLogger(IndexingService.class);

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

                    if(pool.getValue().isShutdown()) {
                        log.info("waiting for workers to finish all current tasks for site " + pool.getKey().getUrl());
                        if (!pool.getValue().awaitTermination(10, TimeUnit.SECONDS)) {
                            log.warn("pool didn't terminate within timeout, releasing lock on the front anyway");
                        }
                        log.info("indexing interrupted by user for site " + pool.getKey().getUrl());
                        it.remove();
                        continue;
                    }

                    if (pool.getValue().isQuiescent()) {
                        synchronized (commonContext.getDatabaseService()) {
                            commonContext.getDatabaseService().setSiteStatusToIndexed(pool.getKey());
                        }
                        log.info("indexing complete for site " + pool.getKey().getUrl());
                        it.remove();
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("something interrupted Indexing-monitor during timeout!");
            throw new RuntimeException(); //TODO: сделать обработку
        }
    };

    public boolean isIndexing() {
        return commonContext.isIndexing();
    }

    public void setIndexing(boolean flag) {
        commonContext.setIndexing(flag);
    }

    public IndexingStatusResponse startIndexing() throws IOException, IndexingStatusException {
        if (commonContext.isIndexing()) {
            throw new IndexingStatusException("Индексация уже запущена");
        }

        commonContext.setIndexing(true);
        commonContext.resetIndexingMessage();

        List<Site> sites = commonContext.getDatabaseService().getSiteRepository().findAll();
        List<Field> fields = commonContext.getDatabaseService().getFieldRepository().findAll();
        sitePools = new HashMap<>();

        for (Site site : sites) {
            addSiteAndStartIndexing(site, Integer.MAX_VALUE, fields);
        }

        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-monitor");
        indexingMonitorTread.start();

        return new IndexingStatusResponse(true, null);
    }

    public IndexingStatusResponse stopIndexing() {
        if (!commonContext.isIndexing()) {
            throw new IndexingStatusException("Индексация уже остановлена");
        }
        commonContext.setIndexing(false);
        commonContext.setIndexingMessage("Индексация прервана пользователем");

        log.info("indexing is stopping, sending termination command to workers");
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

    public IndexingStatusResponse indexOnePage(String url) throws IOException {

        if (commonContext.isIndexing()) {
            throw new IndexingStatusException("Индексация уже запущена");
        }

        sitePools = new HashMap<>();
        List<Field> fields = commonContext.getDatabaseService().getFieldRepository().findAll();
        List<String> userProvidedSitesUrls = userProvidedData.getSites().stream().map(SiteUrlAndNameDTO::getUrl).collect(Collectors.toList());

        String siteUrl = "";
        for (String userUrl : userProvidedSitesUrls) {
            if(url.startsWith(userUrl)) {
                siteUrl = userUrl;
            }
        }
        if (siteUrl.isBlank()) {
            throw new IndexingStatusException("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        commonContext.setIndexing(true);
        commonContext.resetIndexingMessage();

        Site site = commonContext.getDatabaseService().getSiteByUrl(siteUrl);
        addOnePageAndIndex(site, url, fields);
        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-Monitor");
        indexingMonitorTread.start();
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
    }

    public void addOnePageAndIndex(Site site, String url, List<Field> fields) throws IOException {
        CrawlerContext currentContext = generateCrawlerContext(site, 1, fields);
        currentContext.setReindexOnePage(true);
        CrawlerService crawler = new CrawlerService(url, currentContext, commonContext);
        launchIndexing(currentContext, crawler);
    }

    private void launchIndexing(CrawlerContext context, CrawlerService crawler) {
        sitePools.put(context.getSite(), context.getThisPool());
        sitePools.get(context.getSite()).execute(crawler);
    }

    public CrawlerContext generateCrawlerContext (Site site, int limit, List<Field> fields) throws IOException {
        String topLevelSite = getTopLevelUrl(site.getUrl());
        ForkJoinPool pool = new ForkJoinPool();
        BaseRobotRules robotRules = robotsParser.parseContent(topLevelSite + "/robots.txt", getRobotsTxt(topLevelSite), "text/plain", commonContext.getUserAgent());
        return new CrawlerContext(site, pool, limit, new HashSet<>(fields), robotRules);
    }

}

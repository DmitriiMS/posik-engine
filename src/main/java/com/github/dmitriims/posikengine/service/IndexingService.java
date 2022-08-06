package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.exceptions.AsyncIndexingStatusException;
import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import com.github.dmitriims.posikengine.service.crawler.CrawlerService;
import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsMatcher;
import lombok.Getter;
import lombok.Setter;
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
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingService {

    @Resource
    private Parser robotsParser;
    @Resource
    private DatabaseService databaseService;
    @Resource
    private CommonContext commonContext;

    private Map<Site, ForkJoinPool> sitePools;
    @Getter
    private Thread indexingMonitorTread;

    private final Logger log = LoggerFactory.getLogger(IndexingService.class);

    @Setter
    boolean isIndexing = false;

    Runnable indexingMonitor = () -> {
        try {
            while (true) {
                Thread.sleep(1000);
                if (sitePools.isEmpty()) {
                    isIndexing = false;
                    break;
                }

                Iterator<Map.Entry<Site, ForkJoinPool>> it = sitePools.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Site, ForkJoinPool> pool = it.next();
                    if (pool.getValue().isQuiescent()) {
                        pool.getValue().shutdown();
                        synchronized (commonContext.getDatabaseService()) {
                            commonContext.getDatabaseService().setSiteStatusToIndexed(pool.getKey());
                        }
                        log.info("indexing complete for site " + pool.getKey().getUrl());
                        it.remove();
                    }
                }
            }
        } catch (InterruptedException e) {
            log.info("indexing is halted, terminated Indexing-monitor");
        }
    };

    public boolean isIndexing() {
        return isIndexing;
    }

    public IndexingStatusResponse startIndexing() throws IOException, AsyncIndexingStatusException {

        if (isIndexing) {
            throw new AsyncIndexingStatusException(new IndexingStatusResponse(false, "Индексация уже запущена"));
        }
        isIndexing = true;

        List<Site> sites = databaseService.getSiteRepository().findAll();
        List<Field> fields = databaseService.getFieldRepository().findAll();
        sitePools = new HashMap<>();

        for (Site site : sites) {
            String topLevelSite = getTopLevelUrl(site.getUrl());
            addStartIndexingSite(topLevelSite, Integer.MAX_VALUE, fields);
        }

        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-monitor");
        indexingMonitorTread.start();

        return new IndexingStatusResponse(true, null);
    }

    public IndexingStatusResponse stopIndexing() {

        if (!isIndexing) {
            throw new AsyncIndexingStatusException(new IndexingStatusResponse(false, "Индексация уже остановлена"));
        }

        log.info("indexing is stopping, sending termination commands to workers");
        for (Map.Entry<Site, ForkJoinPool> pool : sitePools.entrySet()) {
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

        if (isIndexing) {
            throw new AsyncIndexingStatusException(new IndexingStatusResponse(false, "Индексация уже запущена"));
        }

        sitePools = new HashMap<>();
        List<Field> fields = databaseService.getFieldRepository().findAll();
        String topLevelSite = getTopLevelUrl(url);

        if (!databaseService.getSiteRepository().existsByUrl(topLevelSite)) {
            return new IndexingStatusResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        isIndexing = true;
        addStartIndexingSite(topLevelSite, 1, fields);
        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-Monitor");
        indexingMonitorTread.start();
        return new IndexingStatusResponse(true, null);
    }

    public static byte[] getRobotsTxt(String site) throws IOException {
        byte[] robotsInBytes;
        URL robots = new URL(site + "robots.txt");

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
        return splitSite[0] + "//" + splitSite[1] + "/";
    }

    public void addStartIndexingSite(String topLevelSite, int limit, List<Field> fields) throws IOException {
        Site siteToIndex = databaseService.getSiteRepository().findByUrl(topLevelSite);
        ForkJoinPool pool = new ForkJoinPool();
        RobotsMatcher robotsMatcher = (RobotsMatcher) robotsParser.parse(getRobotsTxt(topLevelSite));
        CrawlerContext currentContext = new CrawlerContext(siteToIndex, pool, limit, new HashSet<>(fields), robotsMatcher);
        CrawlerService crawler = new CrawlerService(currentContext, commonContext);
        sitePools.put(siteToIndex, pool);
        sitePools.get(siteToIndex).execute(crawler);
    }

}

package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.exceptions.AsyncIndexingStatusException;
import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.repositories.FieldRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import com.github.dmitriims.posikengine.service.crawler.CrawlerService;
import com.google.search.robotstxt.Parser;
import com.google.search.robotstxt.RobotsMatcher;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private MorphologyService morphologyService;
    private Parser robotsParser;
    @Getter
    private DatabaseService databaseService;
    private SiteRepository siteRepository;
    private FieldRepository fieldRepository;
    private Map<Site, ForkJoinPool> containers;
    @Getter
    private Thread indexingMonitorTread;

    @Resource
    private CommonContext commonContext;

    private final Logger log = LoggerFactory.getLogger(IndexingService.class);

    @Setter
    boolean isIndexing = false;

    Runnable indexingMonitor = () -> {
        try {
            while (true) {
                Thread.sleep(1000);
                if (containers.isEmpty()) {
                    isIndexing = false;
                    break;
                }

                Iterator<Map.Entry<Site, ForkJoinPool>> it = containers.entrySet().iterator();
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

    @Autowired
    public IndexingService(MorphologyService morphologyService,
                           DatabaseService databaseService,
                           SiteRepository siteRepository,
                           Parser robotsParser,
                           FieldRepository fieldRepository) {
        this.morphologyService = morphologyService;
        this.databaseService = databaseService;
        this.siteRepository = siteRepository;
        this.fieldRepository = fieldRepository;
        this.robotsParser = robotsParser;
    }

    public boolean isIndexing() {
        return isIndexing;
    }

    public void startIndexing() throws IOException, AsyncIndexingStatusException {

        if (isIndexing) {
            throw new AsyncIndexingStatusException("Индексация уже запущена");
        }

        isIndexing = true;
        List<Site> sites = siteRepository.findAll();
        List<Field> fields = fieldRepository.findAll();
        containers = new HashMap<>();


        for (Site site : sites) {
            ForkJoinPool pool = new ForkJoinPool();
            String[] splitSite = site.getUrl().split("//|/");
            String topLevelSite = splitSite[0] + "//" + splitSite[1] + "/";
            RobotsMatcher robotsMatcher = (RobotsMatcher) robotsParser.parse(getRobotsTxt(topLevelSite));
            CrawlerContext currentContext = new CrawlerContext(site, pool, Integer.MAX_VALUE, new HashSet<>(fields), robotsMatcher);
            CrawlerService currentCrawler = new CrawlerService(currentContext, commonContext);
            containers.put(site, pool);
            containers.get(site).execute(currentCrawler);
            log.info(pool.toString());
        }

        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-monitor");
        indexingMonitorTread.start();
    }

    public void stopIndexing() {

        if (!isIndexing) {
            throw new AsyncIndexingStatusException("Индексация не запущена");
        }

        log.info("indexing is stopping, sending termination commands to workers");
        for (Map.Entry<Site, ForkJoinPool> pool : containers.entrySet()) {
            pool.getValue().shutdownNow();
        }
        try {
            indexingMonitorTread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: во всех методах проверять, что флаг индексации корректен. Может кто-то успел начать индексироваться до нас!
    public boolean indexOnePage(String url) throws IOException {

        containers = new HashMap<>();

        List<Field> fields = fieldRepository.findAll();

        String[] splitUrl = url.split("//|/");
        String topLevelSiteFromUrl = splitUrl[0] + "//" + splitUrl[1] + "/";

        if (!siteRepository.existsByUrl(topLevelSiteFromUrl)) {
            return false;
        }

        isIndexing = true;
        Site siteToIndex = siteRepository.findByUrl(topLevelSiteFromUrl);
        ForkJoinPool pool = new ForkJoinPool();
        RobotsMatcher robotsMatcher = (RobotsMatcher) robotsParser.parse(getRobotsTxt(topLevelSiteFromUrl));
        CrawlerContext currentContext = new CrawlerContext(siteToIndex, pool, 1, new HashSet<>(fields), robotsMatcher);
        CrawlerService crawler = new CrawlerService(currentContext, commonContext);
        containers.put(siteToIndex, pool);
        containers.get(siteToIndex).execute(crawler);

        indexingMonitorTread = new Thread(indexingMonitor, "Indexing-Monitor");
        indexingMonitorTread.start();
        return true;
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

}

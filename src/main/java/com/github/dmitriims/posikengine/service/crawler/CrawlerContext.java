package com.github.dmitriims.posikengine.service.crawler;

import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.google.search.robotstxt.RobotsMatcher;
import lombok.Data;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

@Data

public class CrawlerContext {
    private Site site;
    private ForkJoinPool thisPool;
    private Set<String> visitedPages;
    private AtomicInteger numberOfPagesToCrawl;
    private Set<Field> fields;
    private Random delayGenerator;
    private RobotsMatcher robotsMatcher;

    public CrawlerContext(Site site, ForkJoinPool thisPool, int pagesToCrawlLimit, Set<Field> fields, RobotsMatcher robotsMatcher) {
        this.site = site;
        this.thisPool = thisPool;
        this.fields = fields;
        this.robotsMatcher = robotsMatcher;

        this.visitedPages = ConcurrentHashMap.newKeySet();
        this.numberOfPagesToCrawl = new AtomicInteger(pagesToCrawlLimit);
        this.delayGenerator = new Random(System.currentTimeMillis());
    }
}

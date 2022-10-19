package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.model.Site;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class IndexingMonitor implements Runnable {

    private Map<Site, ForkJoinPool> poolsToMonitor;
    private CommonContext commonContext;

    private final Logger log = LoggerFactory.getLogger(IndexingService.class);
    
    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(1000);
                if (poolsToMonitor.isEmpty()) {
                    commonContext.setIndexing(false);
                    break;
                }

                Iterator<Map.Entry<Site, ForkJoinPool>> it = poolsToMonitor.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Site, ForkJoinPool> pool = it.next();

                    if (pool.getValue().isShutdown()) {
                        log.info("ожидаю, пока рабочие завершат все текущие задачи для сайта " + pool.getKey().getUrl());
                        if (!pool.getValue().awaitTermination(10, TimeUnit.SECONDS)) {
                            log.warn("пул не остановился за заданный период ожидания, отпускаю фронт");
                        }
                        synchronized (commonContext.getDatabaseService()) {
                            commonContext.getDatabaseService().cleanSavedPagesCache();
                            commonContext.getDatabaseService().setSiteStatusToFailed(pool.getKey().getId(), "Индексация прервана пользователем");
                        }
                        log.info("индексация прервана для сайта " + pool.getKey().getUrl());
                        commonContext.setAreAllSitesIndexing(false);
                        it.remove();
                        continue;
                    }

                    if (pool.getValue().isQuiescent()) {
                        synchronized (commonContext.getDatabaseService()) {
                            boolean indexedAnything = true;
                            if(commonContext.isIndexingOnePage()) {
                                commonContext.setIndexingOnePage(false);
                            } else {
                                indexedAnything = commonContext.getDatabaseService().removeDeletedPagesForSite(pool.getKey().getId());
                            }
                            if(indexedAnything) {
                                commonContext.getDatabaseService().setSiteStatusToIndexed(pool.getKey().getId());
                            } else {
                                commonContext.getDatabaseService().setSiteStatusToFailed(pool.getKey().getId(), "Ничего не проиндексировано");
                            }
                        }
                        commonContext.setAreAllSitesIndexing(false);
                        log.info("закончена индексация для сайта " + pool.getKey().getUrl());
                        pool.getValue().shutdownNow();
                        it.remove();
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(Thread.currentThread().getName() + " прерван во время ожидания!");
            throw new RuntimeException();
        }
    }
}

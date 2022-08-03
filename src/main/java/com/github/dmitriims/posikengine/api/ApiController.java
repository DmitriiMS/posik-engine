package com.github.dmitriims.posikengine.api;

import com.github.dmitriims.posikengine.dto.statistics.Detailed;
import com.github.dmitriims.posikengine.dto.statistics.Statistics;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.dto.statistics.Total;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.repositories.LemmaRepository;
import com.github.dmitriims.posikengine.repositories.PageRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.IndexingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestController("/api")
public class ApiController {

    SiteRepository siteRepository;
    PageRepository pageRepository;
    LemmaRepository lemmaRepository;

    IndexingService indexingService;

    @Resource
    DatabaseService databaseService;



    @Autowired
    public ApiController(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository,
            IndexingService indexingService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public StatisticsResponse calculateStatistics() {
        boolean isIndexing = siteRepository.existsByStatus(Status.INDEXING);
        Total total = new Total(siteRepository.count(), pageRepository.count(), lemmaRepository.count(), isIndexing);
        List<Detailed> detailed = new ArrayList<>();
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            Detailed d = new Detailed(site.getUrl(),
                    site.getName(),
                    site.getStatusTime().toEpochSecond(ZoneOffset.UTC),
                    site.getLastError(),
                    pageRepository.countBySite(site),
                    lemmaRepository.countBySite(site));
            detailed.add(d);
        }
        Statistics statistics = new Statistics(total, detailed);
        return new StatisticsResponse(true, statistics);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() throws IOException {
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Индексация уже запущена\"}");
        }
        indexingService.startIndexing();
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : true}");
        }
        //TODO: тут надо бросать эксепшн, перехватывать и сбрасывать.
        return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Неизвестная ошибка индексирования\"}");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        if (!indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Индексация не запущена\"}");
        }
        indexingService.stopIndexing();
        if (!indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : true}");
        }
        //TODO: тут надо бросать эксепшн, перехватывать и сбрасывать.
        return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Неизвестная ошибка индексирования\"}");
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(@RequestParam String url) throws IOException {
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Индексация уже запущена\"}");
        }
        if (!indexingService.indexOnePage(url)) {
            return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Данная страница находится за пределами сайтов, указанных в конфигурационном файле\"}");
        }

        if (indexingService.isIndexing()) {
            return ResponseEntity.ok("{\"result\" : true}");
        }
        //TODO: тут надо бросать эксепшн, перехватывать и сбрасывать.
        return ResponseEntity.ok("{\"result\" : false, \"error\" : \"Неизвестная ошибка индексирования\"}");
    }

}

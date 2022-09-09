package com.github.dmitriims.posikengine.api;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.dto.SearchResponse;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.exceptions.IndexingStatusException;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.exceptions.UnknownIndexingStatusException;
import com.github.dmitriims.posikengine.service.IndexingService;
import com.github.dmitriims.posikengine.service.search.SearchService;
import com.github.dmitriims.posikengine.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;

@RestController("/api")
public class ApiController {

    @Resource
    IndexingService indexingService;
    @Resource
    StatisticsService statisticsService;
    @Resource
    SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> calculateStatistics() {
        //TODO: посмотреть, какие могут вылезти ошибки
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingStatusResponse> startIndexing() throws IOException {
        if (indexingService.isIndexing()) {
            throw new IndexingStatusException("Индексация уже запущена");
        }
        IndexingStatusResponse status = indexingService.startIndexing();
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException("Неизвестная ошибка индексирования");
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingStatusResponse> stopIndexing() {
        if (!indexingService.isIndexing()) {
            throw new IndexingStatusException("Индексация не запущена");
        }
        IndexingStatusResponse status = indexingService.stopIndexing();
        if (!indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException("Неизвестная ошибка индексирования");
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingStatusResponse> indexPage(@RequestParam String url) throws IOException {
        if (indexingService.isIndexing()) {
            throw new IndexingStatusException("Индексация уже запущена");
        }
        IndexingStatusResponse status = indexingService.indexOnePage(url);
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException("Неизвестная ошибка индексирования");
    }

    @GetMapping("/search") //TODO: перейти к валидации полей от проверок в методе
    public ResponseEntity<SearchResponse> search(@RequestParam(name = "query") String query,
                                                 @RequestParam(name = "site", required = false) String site,
                                                 @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                 @RequestParam(name = "limit", defaultValue = "20") int limit) throws IOException {
        if (query.isEmpty()) {
            throw new SearchException("Пустой поисковый запрос");
        }

        SearchRequest request = new SearchRequest(query, site, offset, limit);
        Logger log = LoggerFactory.getLogger(ApiController.class);
        log.info(request.toString());
        return ResponseEntity.ok(searchService.search(request));
    }

}

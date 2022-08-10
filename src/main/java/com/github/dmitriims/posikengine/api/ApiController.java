package com.github.dmitriims.posikengine.api;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.dto.SearchResponse;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.exceptions.UnknownIndexingStatusException;
import com.github.dmitriims.posikengine.service.IndexingService;
import com.github.dmitriims.posikengine.service.SearchService;
import com.github.dmitriims.posikengine.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

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
        IndexingStatusResponse status = new IndexingStatusResponse(false, "Индексация уже запущена");

        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }
        status = indexingService.startIndexing();
        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException(new IndexingStatusResponse(false, "Неизвестная ошибка индексирования"));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingStatusResponse> stopIndexing() {
        IndexingStatusResponse status = new IndexingStatusResponse(false, "Индексация не запущена");
        if (!indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }
        status = indexingService.stopIndexing();
        if (!indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException(new IndexingStatusResponse(false, "Неизвестная ошибка индексирования"));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingStatusResponse> indexPage(@RequestParam String url) throws IOException { //TODO: обработка ошибок валидации
        IndexingStatusResponse status = new IndexingStatusResponse(false, "Индексация уже запущена");

        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        status = indexingService.indexOnePage(url);
        if (!status.isResult()) {
            return ResponseEntity.ok(status);
        }

        if (indexingService.isIndexing()) {
            return ResponseEntity.ok(status);
        }

        throw new UnknownIndexingStatusException(new IndexingStatusResponse(false, "Неизвестная ошибка индексирования"));
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
        List<PageDTO> data =  searchService.search(request);
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setError("");
        response.setData(data);
        response.setCount(data.size());
        return ResponseEntity.ok(response);
    }

}

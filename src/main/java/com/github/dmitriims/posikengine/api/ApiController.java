package com.github.dmitriims.posikengine.api;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.exceptions.UnknownIndexingStatusException;
import com.github.dmitriims.posikengine.service.IndexingService;
import com.github.dmitriims.posikengine.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

@RestController("/api")
public class ApiController {

    @Resource
    IndexingService indexingService;
    @Resource
    StatisticsService statisticsService;

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
    public ResponseEntity<IndexingStatusResponse> indexPage(@RequestParam String url) throws IOException {
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

}

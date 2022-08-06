package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.service.IndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.annotation.Resource;

@ControllerAdvice
public class ApiAdvice {

    @Resource
    private IndexingService indexingService;

    @ExceptionHandler(AsyncIndexingStatusException.class)
    public ResponseEntity<String> handleAlreadyIndexingException(AsyncIndexingStatusException ise) {
        return ResponseEntity.ok(ise.getMessage());
    }

    @ExceptionHandler(UnknownIndexingStatusException.class)
    public ResponseEntity<String> handleUnknownIndexingException(UnknownIndexingStatusException uise) {
        indexingService.setIndexing(false);
        indexingService.getIndexingMonitorTread().interrupt();
        indexingService.getDatabaseService().setAllSiteStatusesToFailed("неизвестная ошибка индексирования");
        return ResponseEntity.ok(uise.getMessage());
    }
}

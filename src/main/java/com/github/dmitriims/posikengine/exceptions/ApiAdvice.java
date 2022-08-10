package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.dto.SearchResponse;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.IndexingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.annotation.Resource;
@ControllerAdvice
public class ApiAdvice {

    @Resource
    private IndexingService indexingService;
    @Resource
    private DatabaseService databaseService;

    @ExceptionHandler(AsyncIndexingStatusException.class)
    public ResponseEntity<String> handleAlreadyIndexingException(AsyncIndexingStatusException ise) {
        return ResponseEntity.ok(ise.getMessage());
    }

    @ExceptionHandler(UnknownIndexingStatusException.class)
    public ResponseEntity<String> handleUnknownIndexingException(UnknownIndexingStatusException uise) {
        indexingService.setIndexing(false);
        indexingService.getIndexingMonitorTread().interrupt();
        databaseService.setAllSiteStatusesToFailed("неизвестная ошибка индексирования");
        return ResponseEntity.ok(uise.getMessage());
    }

    @ExceptionHandler(SearchException.class) //TODO:Переделать остальные ошибки так же, это логично и легко
    public ResponseEntity<SearchResponse> handleSearchException(SearchException se) {
        SearchResponse sr = new SearchResponse();
        sr.setResult(false);
        sr.setError(se.getMessage());
        return new ResponseEntity<>(sr, HttpStatus.OK);
    }
}

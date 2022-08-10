package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
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

    @ExceptionHandler(IndexingStatusException.class)
    public ResponseEntity<IndexingStatusResponse> handleAlreadyIndexingException(IndexingStatusException ise) {
        IndexingStatusResponse sr = new IndexingStatusResponse();
        sr.setResult(false);
        sr.setError(ise.getMessage());
        return new ResponseEntity<>(sr, HttpStatus.OK);
    }

    @ExceptionHandler(UnknownIndexingStatusException.class)
    public ResponseEntity<IndexingStatusResponse> handleUnknownIndexingException(UnknownIndexingStatusException uise) {
        indexingService.setIndexing(false);
        indexingService.getIndexingMonitorTread().interrupt();
        databaseService.setAllSiteStatusesToFailed("неизвестная ошибка индексирования");
        IndexingStatusResponse sr = new IndexingStatusResponse();
        sr.setResult(false);
        sr.setError(uise.getMessage());
        return new ResponseEntity<>(sr, HttpStatus.OK);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<SearchResponse> handleSearchException(SearchException se) {
        SearchResponse sr = new SearchResponse();
        sr.setResult(false);
        sr.setError(se.getMessage());
        return new ResponseEntity<>(sr, HttpStatus.OK);
    }
}

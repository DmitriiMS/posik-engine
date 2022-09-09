package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.dto.ErrorResponse;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.IndexingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.ConnectException;

@ControllerAdvice
public class ApiAdvice {

    private IndexingService indexingService;
    private DatabaseService databaseService;

    public ApiAdvice(IndexingService indexingService, DatabaseService databaseService) {
        this.indexingService = indexingService;
        this.databaseService = databaseService;
    }

    @ExceptionHandler(IndexingStatusException.class)
    public ResponseEntity<ErrorResponse> handleIndexingStatusException(IndexingStatusException ise) {
        return new ResponseEntity<>(
                new ErrorResponse(ise.getLocalizedMessage()),
                HttpStatus.OK);
    }

    @ExceptionHandler(UnknownIndexingStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnknownIndexingException(UnknownIndexingStatusException uise) {
        indexingService.setIndexing(false);
        indexingService.getIndexingMonitorTread().interrupt();
        databaseService.setAllSiteStatusesToFailed("неизвестная ошибка индексирования");

        return new ResponseEntity<>(
                new ErrorResponse(uise.getLocalizedMessage()),
                HttpStatus.OK);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ErrorResponse> handleSearchException(SearchException se) {
        return new ResponseEntity<>(
                new ErrorResponse(se.getLocalizedMessage()),
                HttpStatus.OK);
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(ConnectException ce) {
        return new ResponseEntity<>(
                new ErrorResponse(ce.getLocalizedMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

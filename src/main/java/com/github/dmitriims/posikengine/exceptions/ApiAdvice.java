package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.dto.ErrorResponse;
import com.github.dmitriims.posikengine.service.CommonContext;
import com.github.dmitriims.posikengine.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.net.ConnectException;

@ControllerAdvice
public class ApiAdvice {

    private IndexingService indexingService;
    private CommonContext commonContext;

    private final Logger log = LoggerFactory.getLogger(ApiAdvice.class);

    public ApiAdvice(IndexingService indexingService, CommonContext commonContext) {
        this.indexingService = indexingService;
        this.commonContext = commonContext;
    }

    @ExceptionHandler(IndexingStatusException.class)
    public ResponseEntity<ErrorResponse> handleIndexingStatusException(IndexingStatusException ise) {
        log.warn(ise.getLocalizedMessage());
        return new ResponseEntity<>(
                new ErrorResponse(ise.getLocalizedMessage()),
                HttpStatus.OK
        );
    }

    @ExceptionHandler(UnknownIndexingStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnknownIndexingException(UnknownIndexingStatusException uise) {
        log.warn(uise.getLocalizedMessage());
        indexingService.setIndexing(false);
        indexingService.getIndexingMonitorTread().interrupt();

        return new ResponseEntity<>(
                new ErrorResponse(uise.getLocalizedMessage()),
                HttpStatus.OK
        );
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ErrorResponse> handleSearchException(SearchException se) {
        log.warn(se.getLocalizedMessage());
        return new ResponseEntity<>(
                new ErrorResponse(se.getLocalizedMessage()),
                HttpStatus.OK
        );
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(ConnectException ce) {
        log.error(ce.toString());
        if (indexingService.isIndexing()) {
            indexingService.setIndexing(false);
        }
        return new ResponseEntity<>(
                new ErrorResponse(ce.getLocalizedMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(IOException.class)
    public void logIOErrorAndThrowMildOne(IOException ioe) {
        log.error(ioe.toString());
        throw new UnspecifiedInternalError("Произошла неопределённая внутренняя ошибка. Нам очень жаль. Наши админы трудятся в поте лица, чтобы всё исправить! (◡‿◡✿)");
    }

    @ExceptionHandler(NullPointerException.class)
    public void logNullPointerExceptionAndThrowMildOne(NullPointerException npe) {
        log.error(npe.toString());
        throw new UnspecifiedInternalError("Произошла неопределённая внутренняя ошибка. Нам очень жаль. Наши админы трудятся в поте лица, чтобы всё исправить! (◡‿◡✿)");
    }

    @ExceptionHandler(UnspecifiedInternalError.class)
    public ResponseEntity<ErrorResponse> handleUnspecifiedInternalError(UnspecifiedInternalError uie) {
        log.warn(uie.getLocalizedMessage());
        return new ResponseEntity<>(
                new ErrorResponse(uie.getLocalizedMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}

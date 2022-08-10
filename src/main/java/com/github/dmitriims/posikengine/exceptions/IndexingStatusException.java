package com.github.dmitriims.posikengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.OK)
public class IndexingStatusException extends RuntimeException {
    public IndexingStatusException(String message) {
        super(message);
    }
}

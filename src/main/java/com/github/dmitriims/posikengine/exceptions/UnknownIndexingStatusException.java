package com.github.dmitriims.posikengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.OK)
public class UnknownIndexingStatusException extends RuntimeException{
    public UnknownIndexingStatusException(String message) {
        super("{\"result\" : false, \"error\" : \"" + message +"\"}");
    }
}

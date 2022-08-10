package com.github.dmitriims.posikengine.exceptions;

import com.github.dmitriims.posikengine.dto.IndexingStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.OK)
public class AsyncIndexingStatusException extends RuntimeException {
    public AsyncIndexingStatusException(IndexingStatusResponse status) {
        super(status.toString());
    }
}

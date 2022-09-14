package com.github.dmitriims.posikengine.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SearchResponse {
    @NotEmpty
    private boolean result;
    private String message;
    private int count;
    private List<PageResponse> data;
}

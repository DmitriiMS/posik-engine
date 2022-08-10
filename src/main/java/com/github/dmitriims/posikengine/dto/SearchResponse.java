package com.github.dmitriims.posikengine.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SearchResponse {
    @NotEmpty
    private boolean result;
    private String error;
    private int count;
    private List<PageDTO> data;
}

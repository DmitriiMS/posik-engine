package com.github.dmitriims.posikengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {
    @NotEmpty
    private boolean result;
    private String message;
    private int count;
    private List<PageResponse> data;
}

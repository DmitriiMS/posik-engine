package com.github.dmitriims.posikengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {
    @NotEmpty
    private String query;
    @NotEmpty
    private String site;
    private int offset = 0;
    private int limit = 20;
}

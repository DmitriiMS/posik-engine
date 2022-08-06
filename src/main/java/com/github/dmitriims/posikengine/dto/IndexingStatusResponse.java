package com.github.dmitriims.posikengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexingStatusResponse {
    @NotEmpty
    boolean result;
    String error;

    @Override
    public String toString() {
        return "{\"result\": " + this.result + ", \"error\": \"" + this.getError() + "\"}";
    }
}

package com.github.dmitriims.posikengine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO {
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

    @Override
    public String toString() {
        return "uri: " + this.getUri() + "\n" +
                "relevance: " + this.getRelevance() + "\n" +
                "title: " + this.getTitle() + "\n" +
                "snippet:\n" + this.getSnippet() + "\n";
    }
}

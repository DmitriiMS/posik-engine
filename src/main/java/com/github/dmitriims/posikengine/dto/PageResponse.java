package com.github.dmitriims.posikengine.dto;

import lombok.Data;

@Data
public class PageResponse {
    private String site;
    private String siteName;
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

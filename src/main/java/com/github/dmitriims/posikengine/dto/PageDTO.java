package com.github.dmitriims.posikengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageDTO {
    String siteUrl;
    String siteName;
    String path;
    String content;
    double relevance;
}

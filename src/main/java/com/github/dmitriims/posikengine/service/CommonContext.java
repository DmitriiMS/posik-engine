package com.github.dmitriims.posikengine.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class CommonContext {
    private String userAgent;
    private DatabaseService databaseService;
    private List<String> FORBIDDEN_COMPONENTS;
    private MorphologyService morphologyService;

    private boolean isIndexing = false;
    private boolean areAllSitesIndexing = false;
    private boolean isIndexingOnePage = false;
    private String indexingMessage = "";

    public CommonContext(
            @Qualifier("userAgent")
            String userAgent,
            DatabaseService databaseService,
            @Qualifier("forbiddenList")
            List<String> FORBIDDEN_COMPONENTS,
            MorphologyService morphologyService) {
        this.userAgent = userAgent;
        this.databaseService = databaseService;
        this.FORBIDDEN_COMPONENTS = FORBIDDEN_COMPONENTS;
        this.morphologyService = morphologyService;
    }

    public void resetIndexingMessage() {
        indexingMessage = "";
    }
}

package com.github.dmitriims.posikengine.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Data
@Component
public class CommonContext { //TODO: как можно оптимизировать? Не все поля везде нужны
    @Resource(name = "userAgent")
    private String userAgent;
    @Resource
    private DatabaseService databaseService;
    @Resource(name = "forbiddenList")
    private List<String> FORBIDDEN_COMPONENTS;
    @Resource
    private MorphologyService morphologyService;

    private boolean isIndexing = false;
    private String indexingMessage = "";

    public void resetIndexingMessage() {
        indexingMessage = "";
    }
}

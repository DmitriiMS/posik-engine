package com.github.dmitriims.posikengine.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Data
@Component
public class CommonContext {
    @Resource(name = "userAgent")
    private String userAgent;
    @Resource(name = "databaseService")
    private DatabaseService databaseService;
    @Resource(name = "forbiddenList")
    private List<String> FORBIDDEN_COMPONENTS;
    @Resource
    private MorphologyService morphologyService;
}

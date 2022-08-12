package com.github.dmitriims.posikengine.config;

import crawlercommons.robots.SimpleRobotRulesParser;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BeanConfiguration {

    //TODO: here and further look into ways of handling exceptions in a better way
    @Bean(name = "russianMorphology")
    public LuceneMorphology russianMorphology() throws IOException {
        return new RussianLuceneMorphology();
    }

    @Bean(name = "englishMorphology")
    public LuceneMorphology englishMorphology() throws IOException {
        return new EnglishLuceneMorphology();
    }

    @Bean
    public SimpleRobotRulesParser robotsParser() {
        return new SimpleRobotRulesParser();
    }

    @Bean(name = "forbiddenList")
    public List<String> forbiddenElementsList() {
        return new ArrayList<>() {{
            add("#");
            add("mailto:");
            add("tel:");
            add("javascript:");
        }};
    }

    @Bean(name = "userAgent")
    public String userAgent() {
        return "${search-engine-properties.user-agent}";
    }
}

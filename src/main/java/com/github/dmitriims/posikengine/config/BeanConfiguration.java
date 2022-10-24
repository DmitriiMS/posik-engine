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
        return "Mozilla/5.0 (compatible; ${search-engine-properties.user-agent}/1.2 ; +https://github.com/DmitriiMS/posik-engine)";
    }

    @Bean(name = "notAWord")
    public String notAWord() { return "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";}
}

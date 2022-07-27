package com.github.dmitriims.posikengine.config;

import com.github.dmitriims.posikengine.DBConnection;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Component
@ConfigurationProperties(prefix = "search-engine-properties.preload")
@Data
public class DatabasePreloader {

    private List<SiteProperties> sites;
    private Logger log = LoggerFactory.getLogger(DatabasePreloader.class);

    @Data
    public static class SiteProperties {
        private String url;
        private String name;
    }

    @Bean
    CommandLineRunner initDatabase(SiteRepository siteRepository) {
        return args -> {
            //DBConnection.createSiteTable();
            for (SiteProperties sp : sites) {
                Site site = new Site();
                site.setUrl(sp.getUrl());
                site.setName(sp.getName());
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("preloaded, no yet indexed");

                if (!siteRepository.existsByUrl(site.getUrl())) {
                    siteRepository.save(site);
                    log.info("preloaded: " + site);
                }
            }
        };
    }
}

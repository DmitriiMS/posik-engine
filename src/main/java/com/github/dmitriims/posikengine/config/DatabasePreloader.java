package com.github.dmitriims.posikengine.config;

import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.repositories.FieldRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "search-engine-properties.preload")
@Data
public class DatabasePreloader {

    private List<SiteProperties> sites;
    private List<FieldProperties> fields;
    private Logger log = LoggerFactory.getLogger(DatabasePreloader.class);

    @Bean
    @Autowired
    CommandLineRunner initDatabase(SiteRepository siteRepository, FieldRepository fieldRepository) {
        return args -> {
            //DBConnection.createSiteTable();
            for (SiteProperties sp : sites) {
                if (siteRepository.existsByUrl(sp.getUrl())) {
                    continue;
                }
                Site site = new Site();
                site.setUrl(sp.getUrl());
                site.setName(sp.getName());
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                site.setLastError("preloaded, no yet indexed");
                siteRepository.save(site);
                log.info("preloaded site: " + site);
            }


            for (FieldProperties fp : fields) {
                if (fieldRepository.existsByName(fp.getName())) {
                    continue;
                }
                Field field = new Field();
                field.setName(fp.getName());
                field.setSelector(fp.getSelector());
                field.setWeight(fp.getWeight());
                fieldRepository.save(field);
                log.info("preloaded field: " + field);
            }
        };
    }
}

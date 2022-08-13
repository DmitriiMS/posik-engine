package com.github.dmitriims.posikengine.config;

import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import com.github.dmitriims.posikengine.dto.userprovaideddata.FieldDTO;
import com.github.dmitriims.posikengine.dto.userprovaideddata.SiteUrlAndNameDTO;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Configuration
@Data
public class DatabasePreloader {

    @Resource
    private UserProvidedData userProvidedData;
    private Logger log = LoggerFactory.getLogger(DatabasePreloader.class);

    @Bean
    @Autowired
    CommandLineRunner initDatabase(SiteRepository siteRepository, FieldRepository fieldRepository) {
        return args -> {
            for (SiteUrlAndNameDTO sp : userProvidedData.getSites()) {
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


            for (FieldDTO fp : userProvidedData.getFields()) {
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

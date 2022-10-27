package com.github.dmitriims.posikengine.config;

import com.github.dmitriims.posikengine.dto.userprovaideddata.AuthDetailsDTO;
import com.github.dmitriims.posikengine.dto.userprovaideddata.UserProvidedData;
import com.github.dmitriims.posikengine.dto.userprovaideddata.FieldDTO;
import com.github.dmitriims.posikengine.dto.userprovaideddata.SiteUrlAndNameDTO;
import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.model.User;
import com.github.dmitriims.posikengine.repositories.FieldRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import com.github.dmitriims.posikengine.repositories.UserRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Configuration
@Data
public class DatabasePreloader {

    private UserProvidedData userProvidedData;
    private PasswordEncoder passwordEncoder;
    private Logger log = LoggerFactory.getLogger(DatabasePreloader.class);

    public DatabasePreloader(UserProvidedData userProvidedData, PasswordEncoder passwordEncoder) {
        this.userProvidedData = userProvidedData;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Autowired
    CommandLineRunner initDatabase(SiteRepository siteRepository, FieldRepository fieldRepository, UserRepository userRepository) {
        return args -> {
            loadSites(siteRepository);
            loadFields(fieldRepository);
            loadUsers(userRepository);

        };
    }

    private void loadSites(SiteRepository siteRepository) {
        for (SiteUrlAndNameDTO siteDto : userProvidedData.getSites()) {
            if (siteRepository.existsByUrl(siteDto.getUrl())) {
                continue;
            }
            Site site = new Site();
            site.setUrl(siteDto.getUrl());
            site.setName(siteDto.getName());
            site.setStatus(Status.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Ещё не проиндексирован");
            siteRepository.save(site);
            log.info("загружен в базу данных из конфига сайт: " + site);
        }
    }

    private void loadFields(FieldRepository fieldRepository) {
        for (FieldDTO fieldDto : userProvidedData.getFields()) {
            if (fieldRepository.existsByName(fieldDto.getName())) {
                continue;
            }
            Field field = new Field();
            field.setName(fieldDto.getName());
            field.setSelector(fieldDto.getSelector());
            field.setWeight(fieldDto.getWeight());
            fieldRepository.save(field);
            log.info("загружено в базу данных из конфига поле: " + field);
        }
    }

    private void loadUsers(UserRepository userRepository) {
        for (AuthDetailsDTO authDto : userProvidedData.getAuthorisations()) {
            if (userRepository.existsByUsername(authDto.getUsername())) {
                continue;
            }
            User user = new User();
            user.setUsername(authDto.getUsername());
            user.setPassword(passwordEncoder.encode(authDto.getPassword()));
            user.setRole(authDto.getRole());
            userRepository.save(user);
            log.info("загружены в базу данных из конфига данные пользователя: " + authDto.getUsername());
        }
    }
}

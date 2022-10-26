package com.github.dmitriims.posikengine.dto.userprovaideddata;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "search-engine-properties.preload")
@Data
public class UserProvidedData {
    private List<SiteUrlAndNameDTO> sites;
    private List<FieldDTO> fields;
    private List<AuthDetails> authorisations;
}

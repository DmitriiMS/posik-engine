package com.github.dmitriims.posikengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Total {
    private long sites;
    private long pages;
    private long lemmas;
    private boolean isIndexing;
}

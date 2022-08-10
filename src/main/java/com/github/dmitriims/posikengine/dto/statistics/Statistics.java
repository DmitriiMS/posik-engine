package com.github.dmitriims.posikengine.dto.statistics;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public class Statistics {
    private Total total;
    private List<Detailed> detailed;

    @Autowired
    public Statistics(Total total, List<Detailed> detailed) {
        this.total = total;
        this.detailed = detailed;
    }
}

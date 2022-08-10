package com.github.dmitriims.posikengine.dto.statistics;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class StatisticsResponse {
    private boolean result;
    private Statistics statistics;

    public StatisticsResponse(boolean result, @Autowired Statistics statistics) {
        this.result = result;
        this.statistics = statistics;
    }
}

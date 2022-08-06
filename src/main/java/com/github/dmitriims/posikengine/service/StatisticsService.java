package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.statistics.Detailed;
import com.github.dmitriims.posikengine.dto.statistics.Statistics;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.dto.statistics.Total;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.repositories.LemmaRepository;
import com.github.dmitriims.posikengine.repositories.PageRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {
    @Resource
    DatabaseService dbService;

    public StatisticsResponse getStatistics() {
        SiteRepository sr = dbService.getSiteRepository();
        PageRepository pr = dbService.getPageRepository();
        LemmaRepository lr = dbService.getLemmaRepository();

        boolean isIndexing = sr.existsByStatus(Status.INDEXING);
        Total total = new Total(sr.count(), pr.count(), lr.count(), isIndexing);
        List<Detailed> detailed = new ArrayList<>();
        List<Site> sites = sr.findAll();
        for (Site site : sites) {
            Detailed d = new Detailed(site.getUrl(),
                    site.getName(),
                    site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    site.getLastError(),
                    pr.countBySite(site),
                    lr.countBySite(site));
            detailed.add(d);
        }
        Statistics statistics = new Statistics(total, detailed);
        return new StatisticsResponse(true, statistics);
    }
}

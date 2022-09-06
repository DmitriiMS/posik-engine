package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.statistics.Detailed;
import com.github.dmitriims.posikengine.dto.statistics.Statistics;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.dto.statistics.Total;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class StatisticsService {

    DatabaseService dbService;

    public StatisticsResponse getStatistics() {

        boolean isIndexing = dbService.siteExistsByStatus(Status.INDEXING);
        Total total = new Total(dbService.siteCount(), dbService.pageCount(), dbService.lemmaCount(), isIndexing);
        List<Detailed> detailed = new ArrayList<>();
        List<Site> sites = dbService.getAllSites();
        for (Site site : sites) {
            Detailed d = new Detailed(site.getUrl(),
                    site.getName(),
                    site.getStatus(),
                    site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    site.getLastError(),
                    dbService.countPagesBySite(site),
                    dbService.countLemmasBySite(site));
            detailed.add(d);
        }
        detailed.sort(Comparator.comparing(Detailed::getName));
        Statistics statistics = new Statistics(total, detailed);
        return new StatisticsResponse(true, statistics);
    }
}

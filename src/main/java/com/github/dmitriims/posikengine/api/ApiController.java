package com.github.dmitriims.posikengine.api;

import com.github.dmitriims.posikengine.dto.statistics.Detailed;
import com.github.dmitriims.posikengine.dto.statistics.Statistics;
import com.github.dmitriims.posikengine.dto.statistics.StatisticsResponse;
import com.github.dmitriims.posikengine.dto.statistics.Total;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import com.github.dmitriims.posikengine.repositories.LemmaRepository;
import com.github.dmitriims.posikengine.repositories.PageRepository;
import com.github.dmitriims.posikengine.repositories.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestController("/api")
public class ApiController {

    SiteRepository siteRepository;
    PageRepository pageRepository;
    LemmaRepository lemmaRepository;

    @Autowired
    public ApiController(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @GetMapping("/statistics")
    public StatisticsResponse calculateStatistics() {
        boolean isIndexing = siteRepository.existsByStatus(Status.INDEXING);
        Total total = new Total(siteRepository.count(), pageRepository.count(), lemmaRepository.count(), isIndexing);
        List<Detailed> detailed = new ArrayList<>();
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            Detailed d = new Detailed(site.getUrl(),
                    site.getName(),
                    site.getStatusTime().toEpochSecond(ZoneOffset.ofTotalSeconds(0)),
                    site.getLastError(),
                    pageRepository.countBySite(site),
                    lemmaRepository.countBySite(site));
            detailed.add(d);
        }
        Statistics statistics = new Statistics(total, detailed);
        StatisticsResponse statisticsResponse = new StatisticsResponse(true, statistics);
        return statisticsResponse;
    }
}

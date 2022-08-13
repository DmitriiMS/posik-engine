package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.repositories.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Data
@NoArgsConstructor
public class DatabaseService {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private FieldRepository fieldRepository;

    private Logger log = LoggerFactory.getLogger(DatabaseService.class);

    @Autowired
    public DatabaseService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, FieldRepository fieldRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
    }

    @Transactional
    public void savePageToDataBase(Site site, Page page, List<Lemma> lemmas) {
        Page savedPage = pageRepository.save(page);

        for (Lemma lemmaToSave : lemmas) {
            Lemma lemmaToUpdate = lemmaRepository.findByLemma(lemmaToSave.getLemma());
            if (lemmaToUpdate == null) {
                lemmaToUpdate = lemmaToSave;
            } else {
                lemmaToUpdate.setFrequency(lemmaToUpdate.getFrequency() + lemmaToSave.getFrequency());
                lemmaToUpdate.setRank(lemmaToUpdate.getRank() + lemmaToSave.getRank());
            }
            Lemma savedLemma = lemmaRepository.save(lemmaToUpdate);

            Index indexToSave = new Index();
            indexToSave.setPage(savedPage);
            indexToSave.setLemma(savedLemma);
            indexToSave.setRank(savedLemma.getRank());

            Index indexToUpdate = indexRepository.findByPage_IdAndLemma_Id(savedPage.getId(), savedLemma.getId());
            if (indexToUpdate == null) {
                indexToUpdate = indexToSave;
            } else {
                indexToUpdate.setRank(indexToUpdate.getRank() + indexToSave.getRank());
            }

            indexRepository.save(indexToUpdate);
        }

        setSiteStatusToIndexing(site);
    }

    @Transactional
    public Site setSiteStatusToIndexed(Site site) {
        Site siteToUpdate = siteRepository.findById(site.getId()).get();
        siteToUpdate.setStatus(Status.INDEXED);
        siteToUpdate.setLastError("");
        siteToUpdate.setStatusTime(LocalDateTime.now());
        site = siteRepository.save(siteToUpdate);
        return site;
    }

    @Transactional
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    @Transactional
    public Site getSiteByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    @Transactional
    public Site setSiteStatusToIndexing(Site site) {
        Site siteToUpdate = siteRepository.findById(site.getId()).get();
        siteToUpdate.setStatus(Status.INDEXING);
        siteToUpdate.setLastError("");
        siteToUpdate.setStatusTime(LocalDateTime.now());
        site = siteRepository.save(siteToUpdate);
        return site;
    }

    @Transactional
    public void setSiteStatusToFailed(Site site, String error) {
        Site siteToUpdate = siteRepository.findById(site.getId()).get();
        siteToUpdate.setStatus(Status.FAILED);
        siteToUpdate.setLastError(error);
        siteToUpdate.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteToUpdate);
    }

    @Transactional
    public void setAllSiteStatusesToFailed(String error) {
        List<Site> sites = siteRepository.findAll();
        sites.forEach(s -> setSiteStatusToFailed(s, error));
    }

    @Transactional
    public void deleteSiteInformation(Site site) {
        List<Long> test = pageRepository.getAllIdsBySiteId(site.getId());
        for(long id : test) {
            indexRepository.deleteAllByPage_Id(id);
        }
        lemmaRepository.deleteAllBySite(site);
        pageRepository.deleteAllBySite(site);
    }

    @Transactional
    public List<Lemma> filterPopularLemmasOut(List<Site> sites, List<String> lemmas, double threshold) {
        return lemmaRepository.filterVeryPopularLemmas(sites, lemmas, threshold);
    }

    @Transactional
    public List<Page> getPagesWithLemmas(List<Long> lemmasIds, List<Site> sites) {
        List<Long> pagesIds = pageRepository.findAllBySiteIn(sites)
                .stream().map(Page::getId).collect(Collectors.toList());
        for (Long lemmaId : lemmasIds) {
            pagesIds = indexRepository.findAllByPage_IdInAndLemma_Id(pagesIds, lemmaId)
                    .stream().map(i -> i.getPage().getId()).collect(Collectors.toList());
        }
        return pageRepository.findAllByIdIn(pagesIds);
    }

}

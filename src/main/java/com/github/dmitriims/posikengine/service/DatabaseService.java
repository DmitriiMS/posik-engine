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
import java.util.*;
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
    public DatabaseService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository,
                           IndexRepository indexRepository, FieldRepository fieldRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
    }

    @Transactional
    public void savePageToDataBase(Site site, Page page, List<Lemma> lemmas, CommonContext commonContext) {
        Page savedPage = pageRepository.findBySiteAndPathEquals(site, page.getPath());
        if (savedPage == null) {
            savedPage = pageRepository.save(page);
        }
        saveNewLemmasAndIndexes(savedPage, lemmas);
        setIndexingStatusOnCompletion(site, commonContext);
    }

    @Transactional
    public void reindexOnePage(Site site, Page page, List<Lemma> originalLemmas, CommonContext commonContext) {
        Page pageToReindex = pageRepository.findBySiteAndPathEquals(site, page.getPath());
        if(pageToReindex == null) {
            savePageToDataBase(site, page, originalLemmas, commonContext);
            return;
        }
        dropOldIndexesAndDecrementLemmasFrequencies(pageToReindex);
        savePageToDataBase(site, page, originalLemmas, commonContext);
    }

    private void dropOldIndexesAndDecrementLemmasFrequencies(Page page) {
        List<Index> indexesToDelete = indexRepository.findAllByPage_Id(page.getId());
        Lemma lemmaToUpdate;
        int newFrequency;
        for (Index index : indexesToDelete) {
            lemmaToUpdate = index.getLemma();
            newFrequency = lemmaToUpdate.getFrequency() - index.getCount();
            indexRepository.deleteById(index.getId());
            if (newFrequency <= 0) {
                lemmaRepository.deleteById(lemmaToUpdate.getId());
                continue;
            }
            lemmaToUpdate.setFrequency(newFrequency);
            lemmaRepository.save(lemmaToUpdate);
        }
    }

    private void saveNewLemmasAndIndexes(Page page, List<Lemma> newLemmas) {
        for (Lemma newLemma : newLemmas) {
            Lemma lemmaToUpdate = lemmaRepository.findByLemma(newLemma.getLemma());
            if (lemmaToUpdate == null) {
                lemmaToUpdate = newLemma;
            } else {
                lemmaToUpdate.setFrequency(lemmaToUpdate.getFrequency() + newLemma.getFrequency());
                lemmaToUpdate.setRank(lemmaToUpdate.getRank() + newLemma.getRank());
            }
            Lemma savedLemma = lemmaRepository.save(lemmaToUpdate);

            constructAndSaveIndexRecord(page, savedLemma, newLemma.getRank(), newLemma.getFrequency());
        }
    }

    private void constructAndSaveIndexRecord(Page page, Lemma lemma, double rank, int count) {
        Index indexToSave = new Index(page, lemma, rank, count);
        Index indexToUpdate = indexRepository.findByPage_IdAndLemma_Id(page.getId(), lemma.getId());
        if (indexToUpdate == null) {
            indexToUpdate = indexToSave;
        } else {
            indexToUpdate.setRank(indexToSave.getRank());
            indexToUpdate.setCount(indexToSave.getCount());
        }

        indexRepository.save(indexToUpdate);
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
    public Site setSiteStatusToIndexed(Site site) {
        Site siteToUpdate = siteRepository.findById(site.getId()).get();
        siteToUpdate.setStatus(Status.INDEXED);
        siteToUpdate.setLastError("");
        siteToUpdate.setStatusTime(LocalDateTime.now());
        site = siteRepository.save(siteToUpdate);
        return site;
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

    private void setIndexingStatusOnCompletion(Site site, CommonContext commonContext) {
        if(commonContext.isIndexing()) {
            setSiteStatusToIndexing(site);
        } else {
            setSiteStatusToFailed(site, commonContext.getIndexingMessage());
        }
    }

    @Transactional
    public void deleteSiteInformation(Site site) {
        List<Long> pageIds = pageRepository.getAllIdsBySiteId(site.getId());
        for(long id : pageIds) {
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

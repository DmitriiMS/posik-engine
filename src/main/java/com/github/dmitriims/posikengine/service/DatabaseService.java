package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.repositories.*;
import com.github.dmitriims.posikengine.repositories.LemmaRepository;
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
        if (savedPage != null) {
            return; //TODO: проверить возможность этого случая, если да, то в идеале кидать исключение
        }
        savedPage = pageRepository.save(page);
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
        List<Lemma> lemmasFromDB = lemmaRepository.findAllBySiteAndLemmaIn(page.getSite(), newLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));
        List<Lemma> savedLemmas = saveLemmas(newLemmas, lemmasFromDB);
        saveIndexes(page, newLemmas, savedLemmas);
    }

    private List<Lemma> saveLemmas(List<Lemma> newLemmas, List<Lemma> lemmasFromDB) {
        List<Lemma> lemmasToFlush = new ArrayList<>();
        for (Lemma newLemma : newLemmas) {
            int j = lemmasFromDB.indexOf(newLemma);
            if (j < 0) {
                lemmasToFlush.add(newLemma);
                continue;
            }
            Lemma oldLemma = lemmasFromDB.get(j);
            oldLemma.setFrequency(oldLemma.getFrequency() + newLemma.getFrequency());
            lemmasToFlush.add(oldLemma);
        }
        return lemmaRepository.saveAllAndFlush(lemmasToFlush);
    }

    private void saveIndexes (Page page, List<Lemma> newLemmas, List<Lemma> savedLemmas) {
        List<Index> newIndexes = new ArrayList<>();
        for (Lemma lemma : savedLemmas) {
            int i = newLemmas.indexOf(lemma);
            Lemma originalLemma = newLemmas.get(i);
            newIndexes.add(new Index(page, lemma, originalLemma.getRank(),originalLemma.getFrequency()));
        }
        indexRepository.saveAllAndFlush(newIndexes);
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

    @Transactional
    public boolean siteExistsByStatus(Status status) {
        return siteRepository.existsByStatus(status);
    }

    @Transactional
    public long siteCount() {
        return siteRepository.count();
    }

    @Transactional
    public long pageCount() {
        return pageRepository.count();
    }

    @Transactional
    public long lemmaCount() {
        return lemmaRepository.count();
    }

    @Transactional
    public long countPagesBySite(Site site) {
        return pageRepository.countBySite(site);
    }

    @Transactional
    public long countLemmasBySite(Site site) {
        return lemmaRepository.countBySite(site);
    }

    @Transactional
    public List<Field> getAllFields() {
        return fieldRepository.findAll();
    }

}

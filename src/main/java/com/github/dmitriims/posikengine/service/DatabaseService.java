package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.repositories.*;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
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

    private Map<Long, Set<Long>> savedPagesPerSite = new HashMap<>();

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
    public void saveOrUpdatePage(Page page, List<Lemma> originalLemmas, CommonContext commonContext) {
        Page pageToReindex = pageRepository.findBySiteIdAndPagePath(page.getSite().getId(), page.getPath());
        if (pageToReindex == null) {
            savePageToDataBase(page, originalLemmas, commonContext);
            return;
        }
        if (page.equals(pageToReindex)) {
            addPageToSavedPagesMap(pageToReindex);
            setSiteStatusToIndexing(page.getSite().getId());
            return;
        }
        dropOldIndexesAndDecrementLemmasFrequencies(pageToReindex.getId());
        savePageToDataBase(page, originalLemmas, commonContext);
    }


    public void savePageToDataBase(Page page, List<Lemma> lemmas, CommonContext commonContext) {
        Page savedPage = pageRepository.findBySiteAndPathEquals(page.getSite(), page.getPath());

        if (savedPage == null) {
            savedPage = pageRepository.save(page);
        } else if (!savedPage.equals(page)) {
            savedPage.setCode(page.getCode());
            savedPage.setPath(page.getPath());
            savedPage.setContent(page.getContent());
            savedPage.setLemmasHashcode(page.getLemmasHashcode());
            savedPage = pageRepository.saveAndFlush(savedPage);
        }
        addPageToSavedPagesMap(savedPage);
        saveNewLemmasAndIndexes(savedPage, lemmas);
        setIndexingStatusOnCompletion(page.getSite(), commonContext);
    }

    public void addPageToSavedPagesMap(Page page) {
        if (!savedPagesPerSite.containsKey(page.getSite().getId())) {
            savedPagesPerSite.put(page.getSite().getId(), new HashSet<>());
        }
        savedPagesPerSite.get(page.getSite().getId()).add(page.getId());
    }

    @Transactional
    public boolean removeDeletedPagesForSite(Long siteId) {
        Set<Long> savedPagesSet = savedPagesPerSite.get(siteId);
        if(savedPagesSet == null) {
            savedPagesPerSite.remove(siteId);
            return false;
        }
        Set<Long> pagesInDb = new HashSet<>(pageRepository.getAllIdsBySiteId(Collections.singletonList(siteId)));
        if (pagesInDb.size() > savedPagesSet.size()) {
            log.info("удаляю из базы данных страницы, которых больше нет на сайте " + siteRepository.findById(siteId).orElseThrow().getUrl());
            pagesInDb.removeAll(savedPagesSet);
            for (Long pageId : pagesInDb) {
                dropOldIndexesAndDecrementLemmasFrequencies(pageId);
                pageRepository.deleteById(pageId);
            }
            log.info("удаление лишних страниц завершено");

        }
        savedPagesPerSite.remove(siteId);
        return true;
    }

    @Transactional
    public void cleanSavedPagesCache() {
        savedPagesPerSite = new HashMap<>();
    }

    public void dropOldIndexesAndDecrementLemmasFrequencies(Long pageId) {
        List<Index> indexesToDelete = indexRepository.findAllByPage_Id(pageId);
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

    public void saveNewLemmasAndIndexes(Page page, List<Lemma> newLemmas) {
        List<Lemma> lemmasFromDB = lemmaRepository.findAllBySiteAndLemmaIn(page.getSite(), newLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));
        List<Lemma> savedLemmas = saveLemmas(newLemmas, lemmasFromDB);
        saveIndexes(page, newLemmas, savedLemmas);
    }

    public List<Lemma> saveLemmas(List<Lemma> newLemmas, List<Lemma> lemmasFromDB) {
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

    public void saveIndexes(Page page, List<Lemma> newLemmas, List<Lemma> savedLemmas) {
        List<Index> newIndexes = new ArrayList<>();
        for (Lemma lemma : savedLemmas) {
            int i = newLemmas.indexOf(lemma);
            Lemma originalLemma = newLemmas.get(i);
            newIndexes.add(new Index(page, lemma, originalLemma.getRank(), originalLemma.getFrequency()));
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
    public void setSiteStatusToIndexed(Long siteId) {
        Site siteToUpdate = siteRepository.findById(siteId).get();
        siteToUpdate.setStatus(Status.INDEXED);
        siteToUpdate.setLastError("");
        siteToUpdate.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteToUpdate);
    }

    @Transactional
    public void setSiteStatusToIndexing(Long siteId) {
        Site siteToUpdate = siteRepository.findById(siteId).get();
        siteToUpdate.setStatus(Status.INDEXING);
        siteToUpdate.setLastError("");
        siteToUpdate.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteToUpdate);
    }

    @Transactional
    public void setSiteStatusToFailed(Long siteId, String error) {
        Site siteToUpdate = siteRepository.findById(siteId).get();
        siteToUpdate.setStatus(Status.FAILED);
        siteToUpdate.setLastError(error);
        siteToUpdate.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteToUpdate);
    }


    public void setIndexingStatusOnCompletion(Site site, CommonContext commonContext) {
        if (commonContext.isIndexing()) {
            setSiteStatusToIndexing(site.getId());
        } else {
            setSiteStatusToFailed(site.getId(), commonContext.getIndexingMessage());
        }
    }


    @Transactional
    public List<String> filterPopularLemmasOut(List<Site> sites, List<String> lemmas, double threshold) {
        List<Tuple> tuples = lemmaRepository.filterVeryPopularLemmas(
                sites.stream().map(Site::getId).collect(Collectors.toList()),
                lemmas,
                threshold);
        return tuples.stream()
                .map(t -> t.get(0, String.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PageDTO> getSortedRelevantPageDTOs(List<String> lemmas, List<Long> sites, int limit, int offset) {
        List<Long> relevantPages = new ArrayList<>();
        for (String lemma : lemmas) {
            if (relevantPages.isEmpty()) {
                relevantPages = pageRepository.getAllIdsBySiteId(sites);
            }
            relevantPages = indexRepository.findPageIdsBySiteInAndLemmaAndPageIdsIn(sites, lemma, relevantPages);
            if (relevantPages.isEmpty()) {
                return new ArrayList<>();
            }
        }
        return pageRepository.getLimitedSortedPagesByLemmasAndPageIds(lemmas, relevantPages, limit, offset);
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

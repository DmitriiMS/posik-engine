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
import java.util.List;

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
    public Site setSiteStatusToIndexing(Site site) {
        Site siteToUpdate = siteRepository.findById(site.getId()).get();
        siteToUpdate.setStatus(Status.INDEXING);
        siteToUpdate.setStatusTime(LocalDateTime.now());
        site = siteRepository.save(siteToUpdate);
        return site;
    }

    @Transactional
    public void deleteSiteInformation(Site site) {
        List<Long> test = pageRepository.getAllIdsBySiteId(site.getId());
        for(long id : test) {
            indexRepository.deleteAllByPage_Id(id);
        }
        lemmaRepository.deleteAllBySite_Id(site.getId());
        pageRepository.deleteAllBySite_Id(site.getId());
    }

}

package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.PageResponse;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.CommonContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private CommonContext commonContext;

    public SearchService(CommonContext commonContext) {
        this.commonContext = commonContext;
    }

    private static final double THRESHOLD = 0.9;
    private static final Pattern END_OF_SENTENCE = Pattern.compile("[\\.!?]\\s*");

    private final Logger log = LoggerFactory.getLogger(SearchService.class);

    public List<PageResponse> search(SearchRequest request) throws IOException, SearchException {

        String[] searchWordsNormalForms;
        List<Site> sitesToSearch;
        List<PageDTO> foundPages;

        if (request.getSite() == null) {
            sitesToSearch = commonContext.getDatabaseService().getAllSites();
        } else {
            sitesToSearch = new ArrayList<>(){{
                add(commonContext.getDatabaseService().getSiteByUrl(request.getSite()));
            }};
        }

        searchWordsNormalForms = commonContext.getMorphologyService().getAndCountLemmasInString(request.getQuery()).keySet().toArray(new String[0]);

        if (searchWordsNormalForms.length == 0) {
            throw new SearchException("Не удалось выделить леммы для поиска из запроса");
        }
        List<String> filteredLemmas = commonContext.getDatabaseService().filterPopularLemmasOut(sitesToSearch, List.of(searchWordsNormalForms), THRESHOLD);

        if(filteredLemmas.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        foundPages = commonContext.getDatabaseService().getSortedRelevantPageDTOs(filteredLemmas,
                sitesToSearch.stream().map(Site::getId).collect(Collectors.toList()), request.getLimit());

        if (foundPages.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        double maxRelevance = foundPages.get(0).getRelevance();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        PageProcessor pp = new PageProcessor(foundPages, searchWordsNormalForms, maxRelevance, 4, commonContext.getMorphologyService(), END_OF_SENTENCE);
        return pool.submit(pp).join();
    }


}

package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.PageResponse;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.dto.SearchResponse;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.CommonContext;
import com.github.dmitriims.posikengine.service.MorphologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private CommonContext commonContext;

    public SearchService(CommonContext commonContext) {
        this.commonContext = commonContext;
    }

    private static final double THRESHOLD = 0.9;
    private static final String END_OF_SENTENCE = "[\\.!?]\\s*";

    private final Logger log = LoggerFactory.getLogger(SearchService.class);

    public SearchResponse search(SearchRequest request) throws IOException, SearchException {

        String[] searchWordsNormalForms;
        List<Site> sitesToSearch;
        List<PageDTO> foundPages;
        String message = "";

        if (request.getSite() == null) {
            sitesToSearch = commonContext.getDatabaseService().getAllSites();
        } else {
            sitesToSearch = new ArrayList<>() {{
                add(commonContext.getDatabaseService().getSiteByUrl(request.getSite()));
            }};
        }

        searchWordsNormalForms = commonContext.getMorphologyService().getAndCountLemmasInString(request.getQuery()).keySet().toArray(new String[0]);

        if (searchWordsNormalForms.length == 0) {
            throw new SearchException("Не удалось выделить леммы для поиска из запроса");
        }
        List<String> filteredLemmas = commonContext.getDatabaseService().filterPopularLemmasOut(sitesToSearch, List.of(searchWordsNormalForms), THRESHOLD);

        if (filteredLemmas.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        do {
            foundPages = commonContext.getDatabaseService().getSortedRelevantPageDTOs(filteredLemmas,
                    sitesToSearch.stream().map(Site::getId).collect(Collectors.toList()), request.getLimit());
            if (foundPages.size() > 0) {
                break;
            }

            filteredLemmas.remove(0);
        } while (filteredLemmas.size() > 0);

        if (foundPages.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        if (filteredLemmas.size() < searchWordsNormalForms.length) {
            String correctedQuery = correctQuery(filteredLemmas, request.getQuery());
            message = "По запросу \'" + request.getQuery() + "\' ничего не найдено. " +
                    "Скорректированный запрос: " + correctedQuery;
            log.info(message);
        }

        double maxRelevance = foundPages.get(0).getRelevance();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        PageProcessor pp = new PageProcessor(foundPages, filteredLemmas, maxRelevance, 4, commonContext.getMorphologyService(), END_OF_SENTENCE);
        List<PageResponse> searchResults = pool.submit(pp).join();

        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setError(message);
        response.setData(searchResults);
        response.setCount(searchResults.size());

        log.info("search for request \"" + request.getQuery() + "\" complete, found " + searchResults.size() + " pages");
        return response;
    }

    String correctQuery(List<String> lemmas, String originalQuery) {
        MorphologyService ms = commonContext.getMorphologyService();

        String[] splitQuery = ms.splitStringToWords(originalQuery);
        List<String> queryList = new ArrayList<>(List.of(splitQuery));
        List<String> wordNormalForms;

        for (String word : splitQuery) {
            wordNormalForms = ms.getNormalFormOfAWord(word.toLowerCase(Locale.ROOT));
            if (wordNormalForms.isEmpty()) {
                queryList.remove(word);
                continue;
            }
            if (!lemmasContainAnyWordNormalForm(wordNormalForms, lemmas)) {
                queryList.remove(word);
            }
        }
        return String.join(" ", queryList);
    }

    boolean lemmasContainAnyWordNormalForm(List<String> wordNormalForms, List<String> lemmas) {
        for (String word : wordNormalForms) {
            if (lemmas.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
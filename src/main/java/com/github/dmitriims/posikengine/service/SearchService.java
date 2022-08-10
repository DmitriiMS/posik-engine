package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.MorphologyUtils;
import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.model.Index;
import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Page;
import com.github.dmitriims.posikengine.model.Site;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Resource
    private CommonContext commonContext;

    private static final double THRESHOLD = 0.9;
    private static final Pattern END_OF_SENTENCE = Pattern.compile("[\\.!?]\\s*");

    private final Logger log = LoggerFactory.getLogger(SearchService.class);

    public List<PageDTO> search(SearchRequest request) throws IOException, SearchException {

        String[] searchWordsNormalForms;
        List<Site> sitesToSearch;
        List<Page> foundPages;
        List<PageDTO> searchResults = new ArrayList<>();

        if (request.getSite() == null) {
            sitesToSearch = commonContext.getDatabaseService().getAllSites();
        } else {
            sitesToSearch = commonContext.getDatabaseService().getSiteByUrl(request.getSite());
        }

        searchWordsNormalForms = MorphologyUtils.getAndCountLemmasInString(request.getQuery()).keySet().toArray(new String[0]);
        if (searchWordsNormalForms.length == 0) {
            throw new SearchException("Не удалось выделить леммы для поиска из запроса");
        }
        List<Lemma> lemmas = commonContext.getDatabaseService().filterPopularLemmasOut(sitesToSearch, List.of(searchWordsNormalForms), THRESHOLD);
        if(lemmas.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        foundPages = commonContext.getDatabaseService().getPagesWithLemmas(lemmas.stream().map(Lemma::getId).collect(Collectors.toList()), sitesToSearch);

        if (foundPages.size() == 0) {
            throw new SearchException("По запросу \'" + request.getQuery() + "\' ничего не найдено");
        }

        double maxRank = 0.0;
        for (Page page : foundPages) {
            PageDTO pageDTO = convertPageToDto(page, searchWordsNormalForms);
            maxRank = Math.max(maxRank, pageDTO.getRelevance());
            searchResults.add(pageDTO);
        }

        claculateRelevance(searchResults, maxRank);

        return searchResults.stream()
                .sorted(Comparator.comparing(PageDTO::getRelevance, Comparator.reverseOrder()))
                .limit(request.getLimit())
                .collect(Collectors.toList());
    }

    public PageDTO convertPageToDto(Page page, String[] searchWordsNormalForms) {
        PageDTO pageDTO = new PageDTO();
        Document contents = Jsoup.parse(page.getContent());

        pageDTO.setSite(page.getSite().getUrl());
        pageDTO.setSiteName(page.getSite().getName());
        pageDTO.setUri(page.getPath());
        pageDTO.setTitle(contents.select("title").text());
        pageDTO.setSnippet(getSnippetFromPage(contents.select("body").text(), searchWordsNormalForms));
        pageDTO.setRelevance(sumAllRanks(page.getIndices()));

        return pageDTO;
    }

    public double sumAllRanks(Set<Index> indices) {
        double sum = 0.0;
        for (Index index : indices) {
            sum += index.getRank();
        }
        return sum;
    }

    public void claculateRelevance(List<PageDTO> pageDtos, double maxRank) {
        pageDtos.forEach(pdto -> pdto.setRelevance(pdto.getRelevance()/maxRank));
    }

    //TODO: посмотреть как можно упростить.
    public String getSnippetFromPage(String text, String[] searchWordsNormalForms) {
        List<String> sentences = new ArrayList<>();
        Map<String, Integer> wordsWithPositions = getIndexMap(text,searchWordsNormalForms);
        List<String> words = new ArrayList<>(wordsWithPositions.keySet());
        for (Map.Entry<String, Integer> p: wordsWithPositions.entrySet()) {
            if (!words.contains(p.getKey())) {
                continue;
            }
            String sentence = getSentenceWithWordFromText(text, p.getValue(), p.getKey().length());
            ListIterator<String> iterator = words.listIterator();
            while (iterator.hasNext()) {
                String word = iterator.next();
                int index = findIndexOfWord(sentence, word);
                if (index >= 0 && !word.equals(p.getKey())) {
                    sentence = getSentenceWithWordFromText(sentence, index, word.length());
                    iterator.remove();
                }
            }
            sentences.add(sentence);
        }
        return String.join(" <...> ", sentences);
    }

    public Map<String, Integer> getIndexMap(String text, String[] searchWordsNormalForms) {
        Map<String, Integer> result = new TreeMap<>();
        Set<String> normalizedSearchTerms = new HashSet<>(List.of(searchWordsNormalForms));
        String[] words = MorphologyUtils.splitStringToLowercaseWords(text);
        for (String word : words) {
            if(word.isBlank()) {
                continue;
            }
            List<String> normWords = MorphologyUtils.getNormalFormOfAWord(word);
            for (String normWord : normWords) {
                if (normalizedSearchTerms.contains(normWord)) {
                    int index = findIndexOfWord(text, word);
                    if (index < 0) { break; }
                    result.putIfAbsent(word, index);
                    normalizedSearchTerms.remove(normWord);
                    break;
                }
            }
        }
        return result;
    }

    public int findIndexOfWord(String text, String word) {
        Pattern wordPattern = Pattern.compile("\\b" + word + "\\b");
        Matcher wordMatcher = wordPattern.matcher(text.toLowerCase(Locale.ROOT));
        if (!wordMatcher.find()) {
            return -1;
        }
        return wordMatcher.start();
    }

    public String getSentenceWithWordFromText(String text, int wordStartIndex, int wordLength) {
        StringBuilder builder = new StringBuilder();

        String textChunk = text.substring(0, wordStartIndex);
        Matcher matcher = END_OF_SENTENCE.matcher(textChunk);
        int tempIndex = 0;
        while(matcher.find()){
            tempIndex = matcher.end();
        }
        builder.append(text, tempIndex, wordStartIndex);
        textChunk = text.substring(wordStartIndex+wordLength);
        matcher = END_OF_SENTENCE.matcher(textChunk);
        matcher.find();
        tempIndex = matcher.start() + wordStartIndex + wordLength + 1;
        builder.append("<b>").append(text, wordStartIndex, wordStartIndex+wordLength).append("</b>");
        builder.append(text, wordStartIndex+wordLength, tempIndex);
        return builder.toString();
    }
}

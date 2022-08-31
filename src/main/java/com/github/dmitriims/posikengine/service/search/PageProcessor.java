package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.PageResponse;
import com.github.dmitriims.posikengine.service.MorphologyService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PageProcessor  extends RecursiveTask<List<PageResponse>> {

    List<PageDTO> pagesToProcess;
    String[] searchWordsNormalForms;
    double maxRelevance;
    int chunkSize;
    MorphologyService morphologyService;
    Pattern END_OF_SENTENCE;

    @Override
    protected List<PageResponse> compute() {
        if (pagesToProcess.size() <= chunkSize) {
            return processPages();
        } else {
            return ForkJoinTask.invokeAll(splitTasks())
                    .stream()
                    .map(ForkJoinTask::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

    List<PageProcessor> splitTasks() {
        List<PageProcessor> subtasks = new ArrayList<>();
        for (int i = 0; i < pagesToProcess.size(); i += chunkSize) {
            int endIndex = i + chunkSize;
            if (endIndex > pagesToProcess.size()) {
                endIndex = pagesToProcess.size();
            }
            subtasks.add(new PageProcessor(pagesToProcess.subList(i, endIndex), searchWordsNormalForms, maxRelevance, chunkSize, morphologyService, END_OF_SENTENCE));
        }
        return subtasks;
    }

    List<PageResponse> processPages() {
        List<PageResponse> result = new ArrayList<>();
        for (PageDTO page : pagesToProcess) {
            PageResponse pageResponse = convertPageDtoToResponse(page, maxRelevance, searchWordsNormalForms);
            result.add(pageResponse);
        }

        return result;
    }

    public PageResponse convertPageDtoToResponse(PageDTO page, double maxRelevance, String[] searchWordsNormalForms) {

        PageResponse pageResponse = new PageResponse();
        Document contents = Jsoup.parse(page.getContent());

        pageResponse.setSite(page.getSiteUrl());
        pageResponse.setSiteName(page.getSiteName());
        pageResponse.setUri(page.getPath());
        pageResponse.setTitle(contents.select("title").text());
        pageResponse.setSnippet(getSnippetFromPage(contents.select("body").text(), searchWordsNormalForms));
        pageResponse.setRelevance(page.getRelevance() / maxRelevance);

        return pageResponse;
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
        String[] words = morphologyService.splitStringToLowercaseWords(text);
        for (String word : words) {
            if(word.isBlank()) {
                continue;
            }
            List<String> normWords = morphologyService.getNormalFormOfAWord(word);
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
        textChunk = text.substring(wordStartIndex + wordLength);
        matcher = END_OF_SENTENCE.matcher(textChunk);
        matcher.find();
        tempIndex = matcher.start() + wordStartIndex + wordLength + 1;
        builder.append("<b>").append(text, wordStartIndex, wordStartIndex + wordLength).append("</b>");
        builder.append(text, wordStartIndex + wordLength, tempIndex);
        return builder.toString();
    }
}

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
import java.util.stream.Collectors;

@AllArgsConstructor
public class PageProcessor  extends RecursiveTask<List<PageResponse>> {

    private List<PageDTO> pagesToProcess;
    private List<String> searchLemmas;
    private double maxRelevance;
    private int chunkSize;
    private MorphologyService morphologyService;
    private String END_OF_SENTENCE;

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
            subtasks.add(new PageProcessor(pagesToProcess.subList(i, endIndex), searchLemmas, maxRelevance, chunkSize, morphologyService, END_OF_SENTENCE));
        }
        return subtasks;
    }

    List<PageResponse> processPages() {
        List<PageResponse> result = new ArrayList<>();
        for (PageDTO page : pagesToProcess) {
            PageResponse pageResponse = convertPageDtoToResponse(page, maxRelevance, searchLemmas);
            result.add(pageResponse);
        }

        return result;
    }

    PageResponse convertPageDtoToResponse(PageDTO page, double maxRelevance, List<String> searchLemmas) {

        PageResponse pageResponse = new PageResponse();
        Document contents = Jsoup.parse(page.getContent());

        pageResponse.setSite(page.getSiteUrl());
        pageResponse.setSiteName(page.getSiteName());
        pageResponse.setUri(page.getPath());
        pageResponse.setTitle(contents.select("title").text());
        pageResponse.setSnippet(getSnippetFromPage(contents.select("body").text(), searchLemmas));
        pageResponse.setRelevance(page.getRelevance() / maxRelevance);

        return pageResponse;
    }

    String getSnippetFromPage(String text, List<String> searchLemmas) {
        List<String> result = new ArrayList<>();

        List<String> iterableSearchTerms = new ArrayList<>(searchLemmas);

        String[] sentences = text.split(END_OF_SENTENCE);
        ListIterator<String> iterator;
        boolean isFound = false;
        for (String sentence : sentences) {
            for (String word : morphologyService.splitStringToWords(sentence)) {
                iterator = iterableSearchTerms.listIterator();
                String lowercaseWord = word.toLowerCase(Locale.ROOT);
                while(iterator.hasNext()) {
                    String searchTerm = iterator.next();
                    for (String wordNormalForm : morphologyService.getNormalFormOfAWord(lowercaseWord)) {
                        if (!wordNormalForm.equals(searchTerm)) {
                            continue;
                        }
                        isFound = true;
                        sentence = sentence.replaceFirst(word, "<b>" + word + "</b>");
                        iterator.remove();
                    }
                }
            }
            if (isFound) {
                isFound = false;
                result.add(sentence);
                if (iterableSearchTerms.isEmpty()) {
                    break;
                }
            }
        }

        return String.join("<...>", result);
    }
}
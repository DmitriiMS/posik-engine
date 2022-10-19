package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.FoundPage;
import com.github.dmitriims.posikengine.service.MorphologyService;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PageProcessor extends RecursiveTask<List<FoundPage>> {

    private List<PageDTO> pagesToProcess;
    private List<String> searchQuery;
    private double maxRelevance;
    private int chunkSize;
    private MorphologyService morphologyService;

    @Override
    protected List<FoundPage> compute() {
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
            int endIndex = Math.min(i + chunkSize, pagesToProcess.size());
            subtasks.add(new PageProcessor(pagesToProcess.subList(i, endIndex), searchQuery, maxRelevance, chunkSize, morphologyService));
        }
        return subtasks;
    }

    List<FoundPage> processPages() {
        List<FoundPage> result = new ArrayList<>();
        for (PageDTO page : pagesToProcess) {
            FoundPage foundPage = convertPageDtoToResponse(page, maxRelevance, searchQuery);
            result.add(foundPage);
        }

        return result;
    }

    FoundPage convertPageDtoToResponse(PageDTO page, double maxRelevance, List<String> searchQuery) {

        FoundPage foundPage = new FoundPage();
        Document content = Jsoup.parse(page.getContent());

        foundPage.setSite(page.getSiteUrl());
        foundPage.setSiteName(page.getSiteName());
        foundPage.setUri(page.getPath());
        foundPage.setTitle(content.select("title").text());
        foundPage.setSnippet(getSnippetFromPage(content.select("body").text(), searchQuery));
        foundPage.setRelevance(page.getRelevance() / maxRelevance);

        return foundPage;
    }

    String getSnippetFromPage(String text, List<String> searchQuery) {
        List<String> queryLocalCopy = new ArrayList<>(searchQuery);
        List<String> words = List.of(text.split("\\b"));
        ListIterator<String> iterator;
        List<Integer> foundWordsIndexes = new ArrayList<>();

        for (String word : words) {
            if(queryLocalCopy.isEmpty()) {
                break;
            }
            iterator = queryLocalCopy.listIterator();
            while(iterator.hasNext()) {
                List<String> wordNormalForm = new ArrayList<>(morphologyService.getNormalFormOfAWord(word.toLowerCase(Locale.ROOT)));
                wordNormalForm.retainAll(morphologyService.getNormalFormOfAWord(iterator.next()));
                if(wordNormalForm.isEmpty()) {
                    continue;
                }
                foundWordsIndexes.add(words.indexOf(word));
                iterator.remove();
            }
        }

        return constructSnippetWithHighlight(foundWordsIndexes, new ArrayList<>(words));
    }

    String constructSnippetWithHighlight(List<Integer> foundWordsIndexes, List<String> words) {
        List<String> snippetCollector = new ArrayList<>();
        int beginning, end, before, after, index, prevIndex;
        before = 12;
        after = 6;

        foundWordsIndexes.sort(Integer::compareTo);

        for(int i : foundWordsIndexes) {
            words.set(i, "<b>" + words.get(i) + "</b>");
        }

        index = foundWordsIndexes.get(0);
        beginning = Math.max(0, index - before);
        end = Math.min(words.size() - 1, index + after);

        for (int i = 1; i <= foundWordsIndexes.size(); i++) {
            if(i == foundWordsIndexes.size()) {
                snippetCollector.add(String.join("", words.subList(beginning, end)));
                break;
            }
            prevIndex = index;
            index = foundWordsIndexes.get(i);
            if(index - before <= prevIndex) {
                end = Math.min(words.size() - 1, index + after);
                continue;
            }
            snippetCollector.add(String.join("", words.subList(beginning, end)));
            beginning = Math.max(0, index - before);
            end = Math.min(words.size() - 1, index + after);
        }
        return String.join("...", snippetCollector);
    }
}

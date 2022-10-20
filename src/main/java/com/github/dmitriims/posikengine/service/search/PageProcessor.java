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
        foundPage.setSnippet(SnippetBuilder.getSnippetFromPage(morphologyService, content.select("body").text(), searchQuery));
        foundPage.setRelevance(page.getRelevance() / maxRelevance);

        return foundPage;
    }


}

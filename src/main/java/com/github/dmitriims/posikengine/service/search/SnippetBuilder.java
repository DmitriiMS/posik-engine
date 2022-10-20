package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.service.MorphologyService;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class SnippetBuilder {

    public static String getSnippetFromPage(MorphologyService ms, String text, List<String> searchQuery) {
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
                List<String> wordNormalForm = new ArrayList<>(ms.getNormalFormOfAWord(word.toLowerCase(Locale.ROOT)));
                wordNormalForm.retainAll(ms.getNormalFormOfAWord(iterator.next()));
                if(wordNormalForm.isEmpty()) {
                    continue;
                }
                foundWordsIndexes.add(words.indexOf(word));
                iterator.remove();
            }
        }

        return constructSnippetWithHighlight(foundWordsIndexes, new ArrayList<>(words));
    }

    public static String constructSnippetWithHighlight(List<Integer> foundWordsIndexes, List<String> words) {
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

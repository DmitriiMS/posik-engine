package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.service.MorphologyService;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PageProcessorTest {
    LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
    LuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
    String notAWord = "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";

    MorphologyService morphologyService = new MorphologyService(notAWord, russianLuceneMorphology, englishLuceneMorphology);


    PageProcessor pageProcessor;

    public PageProcessorTest() throws IOException {
    }

    @BeforeEach
    public void init() {
        pageProcessor = new PageProcessor(new ArrayList<>(),
                new ArrayList<>(),
                10.,
                2,
                morphologyService,
                "[\\.!?]\\s*");
    }

    @Test
    @DisplayName("getSnippetFromPage - все слова в одном предожении")
    public void getSnippetFromPageAllInOne() {
        List<String> lemmas = new ArrayList<>() {{
            add("мама");
            add("мыть");
            add("рама");
        }};
        String text = "Мама мыла раму.";
        String expected = "<b>Мама</b> <b>мыла</b> <b>раму</b>";
        String actual = pageProcessor.getSnippetFromPage(text, lemmas);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("getSnippetFromPage - все слова в разных предложениях")
    public void getSnippetFromPageInDifferent() {
        List<String> lemmas = new ArrayList<>() {{
            add("мама");
            add("мыть");
            add("рама");
        }};
        String text = "Мама Кузьмы тяжело вздохнула. " +
                "Ведро было полное. " +
                "Снова пора мыть! " +
                "Злые голуби опять испачкали всю раму.";
        String expected = "<b>Мама</b> Кузьмы тяжело вздохнула<...>" +
                "Снова пора <b>мыть</b><...>" +
                "Злые голуби опять испачкали всю <b>раму</b>";
        String actual = pageProcessor.getSnippetFromPage(text, lemmas);

        assertEquals(expected, actual);
    }
}

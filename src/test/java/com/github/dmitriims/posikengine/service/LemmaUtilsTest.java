package com.github.dmitriims.posikengine.service;

import com.github.dmitriims.posikengine.model.Field;
import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.crawler.CrawlerContext;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LemmaUtilsTest {

    @Mock
    CrawlerContext crawlerContext;

    LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
    LuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
    String notAWord = "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";

    MorphologyService morphologyService = new MorphologyService(notAWord, russianLuceneMorphology, englishLuceneMorphology);

    public LemmaUtilsTest() throws IOException {
    }

    Set<Field> fields;
    Site site;

    @BeforeEach
    public void init() throws IOException {

        Field title = new Field();
        title.setName("title");
        title.setSelector("title");
        title.setWeight(1.);
        Field h1 = new Field();
        h1.setName("h1");
        h1.setSelector("h1");
        h1.setWeight(0.9);
        Field body = new Field();
        body.setName("body");
        body.setSelector("body");
        body.setWeight(0.8);
        fields = new HashSet<>(){{
            add(title);
            add(h1);
            add(body);
        }};
    }

    @Test
    @DisplayName("getAndRankAllLemmas - одна лемма в title")
    public void testGetAndRankAllLemmasOneTitle() throws IOException {
        String html = "<html><head><title>title!</title></head><body></body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        Lemma expectedLemma = new Lemma();
        expectedLemma.setSite(site);
        expectedLemma.setLemma("title");
        expectedLemma.setFrequency(1);
        expectedLemma.setRank(1.0);
        List<Lemma> expected = new ArrayList<>(){{
            add(expectedLemma);
        }};

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);

        assertAll("не должно быть лишних лемм, они равны и у них совпадают частота и ранг",
                () -> assertEquals(expected.size(), actual.size(), "размер массивов должен быть одинаковый"),
                () -> assertEquals(expected.get(0), actual.get(0), "леммы должны быть равны (по equals)"),
                () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты должны быть равны"),
                () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги должны быть равны"));
    }

    @Test
    @DisplayName("getAndRankAllLemmas - одна лемма в body")
    public void testGetAndRankAllLemmasOneBody() throws IOException {
        String html = "<html><head><title></title></head><body>BODY!</body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        Lemma expectedLemma = new Lemma();
        expectedLemma.setSite(site);
        expectedLemma.setLemma("body");
        expectedLemma.setFrequency(1);
        expectedLemma.setRank(0.8);
        List<Lemma> expected = new ArrayList<>(){{
            add(expectedLemma);
        }};

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);

        assertAll("не должно быть лишних лемм, они равны и у них совпадают частота и ранг",
                () -> assertEquals(expected.size(), actual.size(), "размер массивов должен быть одинаковый"),
                () -> assertEquals(expected.get(0), actual.get(0), "леммы должны быть равны (по equals)"),
                () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты должны быть равны"),
                () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги должны быть равны"));

    }

    @Test
    @DisplayName("getAndRankAllLemmas - одна лемма и в title и в body")
    public void testGetAndRankAllLemmasOneInBoth() throws IOException {
        String html = "<html><head><title>Parse?</title></head><body>Parsed!</body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        Lemma expectedLemma = new Lemma();
        expectedLemma.setSite(site);
        expectedLemma.setLemma("parse");
        expectedLemma.setFrequency(2);
        expectedLemma.setRank(1.8);
        List<Lemma> expected = new ArrayList<>(){{
            add(expectedLemma);
        }};

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);

        assertAll("не должно быть лишних лемм, они равны и у них совпадают частота и ранг",
                () -> assertEquals(expected.size(), actual.size(), "размер массивов должен быть одинаковый"),
                () -> assertEquals(expected.get(0), actual.get(0), "леммы должны быть равны (по equals)"),
                () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты должны быть равны"),
                () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги должны быть равны"));

    }

    @Test
    @DisplayName("getAndRankAllLemmas - лемм нет")
    public void testGetAndRankAllLemmasNoLemmas() throws IOException {
        String html = "<html><head><title>:%!)))</title></head><body>to the 下!</body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        List<Lemma> expected = new ArrayList<>();

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("getAndRankAllLemmas - две леммы, одна везде, вторая в теле")
    public void testGetAndRankAllLemmasTwoLemmas() throws IOException {
        String html = "<html><head><title>Title Title</title></head><body>Parsed parsed title!</body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        Lemma lemma1 = new Lemma();
        lemma1.setSite(site);
        lemma1.setLemma("parse");
        lemma1.setFrequency(2);
        lemma1.setRank(1.6);

        Lemma lemma2 = new Lemma();
        lemma2.setSite(site);
        lemma2.setLemma("title");
        lemma2.setFrequency(3);
        lemma2.setRank(2.8);

        List<Lemma> expected = new ArrayList<>(){{
            add(lemma1);
            add(lemma2);
        }};

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);

        actual.sort(Comparator.comparing(Lemma::getFrequency));

        assertAll("Массивы должны быть одинаковыми по equals и у лемм должны совпадать частоты и ранги",
                () -> assertIterableEquals(expected, actual, "массивы должны совпадать"),
                () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты первых лемм должны быть равны"),
                () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги первых лемм должны быть равны"),
                () -> assertEquals(expected.get(1).getFrequency(),actual.get(1).getFrequency(), "частоты вторых лемм должны быть равны"),
                () -> assertEquals(expected.get(1).getRank(),actual.get(1).getRank(), "ранги вторых лемм должны быть равны"));
    }

    @Test
    @DisplayName("getAndRankAllLemmas - две леммы, одна в title, одна в h1 и body")
    public void testGetAndRankAllLemmasThreeLemmasWithH1() throws IOException {
        String html = "<html><head><title>Title</title></head><body><h1>body</h1>body</body></html>";
        Document doc = Jsoup.parse(html);

        Mockito.when(crawlerContext.getFields()).thenReturn(fields);

        Lemma lemma1 = new Lemma();
        lemma1.setSite(site);
        lemma1.setLemma("body");
        lemma1.setFrequency(2);
        lemma1.setRank(1.7);

        Lemma lemma2 = new Lemma();
        lemma2.setSite(site);
        lemma2.setLemma("title");
        lemma2.setFrequency(1);
        lemma2.setRank(1);

        List<Lemma> expected = new ArrayList<>(){{
            add(lemma2);
            add(lemma1);
        }};
        String expectedHtml = "<html><head><title>Title</title></head><body>body</body></html>";

        List<Lemma> actual = LemmaUtils.getAndRankAllLemmas(doc, crawlerContext, morphologyService);
        String actualHtml = doc.outerHtml().replaceAll("\n|\s", "");

        actual.sort(Comparator.comparing(Lemma::getFrequency));

        assertAll("Массивы должны быть одинаковыми по equals и у лемм должны совпадать частоты и ранги, из документа должен быть удалён тег h1",
                () -> assertIterableEquals(expected, actual, "массивы должны совпадать"),
                () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты первых лемм должны быть равны"),
                () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги первых лемм должны быть равны"),
                () -> assertEquals(expected.get(1).getFrequency(),actual.get(1).getFrequency(), "частоты вторых лемм должны быть равны"),
                () -> assertEquals(expected.get(1).getRank(),actual.get(1).getRank(), "ранги вторых лемм должны быть равны"),
                () -> assertEquals(expectedHtml, actualHtml, "h1 должен быть удалён из документа"));
    }

    @Test
    @DisplayName("getAndCountLemmasInString - подсчёт лемм")
    public void testGetAndCountLemmasInStringNormal() {
        Map<String, Integer> expected  = new TreeMap<>(){{
            put("42", 1);
            put("be", 1);
            put("first", 3);
            put("parse", 2);
            put("we", 1);
            put("which", 1);
            put("word", 1);
        }};
        Map<String, Integer> actual = LemmaUtils.
                getAndCountNormalFormsInString("First we parsed the first word which was the first to parse. Но не 42!", morphologyService);
        assertIterableEquals(expected.entrySet(), actual.entrySet());
    }
    @Test
    @DisplayName("getAndCountLemmasInString - если мусор, то ничего не вернёт")
    public void testGetAndCountLemmasInStringGarbage() {
        Map<String, Integer> expected  = new TreeMap<>();
        Map<String, Integer> actual = LemmaUtils.getAndCountNormalFormsInString("猫 или 鯨? Тьфу drat!", morphologyService);
        assertIterableEquals(expected.entrySet(), actual.entrySet());
    }
}

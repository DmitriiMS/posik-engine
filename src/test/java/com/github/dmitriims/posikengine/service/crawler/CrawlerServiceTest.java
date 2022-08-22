package com.github.dmitriims.posikengine.service.crawler;

import com.github.dmitriims.posikengine.exceptions.IndexingStatusException;
import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.service.CommonContext;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.MorphologyService;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CrawlerServiceTest {

    @Mock
    CommonContext commonContext;

    @Mock
    CrawlerContext crawlerContext;

    @Mock
    BaseRobotRules rules;

    @Mock
    Connection.Response response;

    @Mock
    DatabaseService databaseService;

    Site site;
    CrawlerService crawler;


    @BeforeEach
    public void init() {
        site = new Site();
        site.setUrl("http://test.test");
        Mockito.when(crawlerContext.getSite()).thenReturn(site);
        crawler = new CrawlerService(crawlerContext, commonContext);
    }

    @Test
    @DisplayName("isSiteIndexedOrInterrupted - indexed")
    public void testIsSiteIndexedOrInterruptedIndexed() {
        site.setStatus(Status.INDEXED);

        assertTrue(crawler.isSiteIndexedOrInterrupted(site));
    }

    @Test
    @DisplayName("isSiteIndexedOrInterrupted - interrupted by user")
    public void testIsSiteIndexedOrInterruptedInterrupted() {
        site.setStatus(Status.FAILED);
        site.setLastError("Индексация прервана пользователем");

        assertTrue(crawler.isSiteIndexedOrInterrupted(site));
    }

    @Test
    @DisplayName("isSiteIndexedOrInterrupted - failed")
    public void testIsSiteIndexedOrInterruptedFailed() {
        site.setStatus(Status.FAILED);
        site.setLastError("еггог");

        assertFalse(crawler.isSiteIndexedOrInterrupted(site));
    }

    @Test
    @DisplayName("isSiteIndexedOrInterrupted - indexing")
    public void testIsSiteIndexedOrInterruptedIndexing() {
        site.setStatus(Status.INDEXING);

        assertFalse(crawler.isSiteIndexedOrInterrupted(site));
    }

    @Test
    @DisplayName("decodeLink - закодированная url")
    public void testDecodeLinkGarbled() {
        String input = "https://www.lutherancathedral.ru/2022/04/16/%D0%BF%D0%B0%D1%81%D1%85%D0%B0%D0%BB%D1%8C%D0%BD%D0%BE%D0%B5-%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82%D1%81%D1%82%D0%B2%D0%B8%D0%B5-%D0%B0%D1%80%D1%85%D0%B8%D0%B5%D0%BF%D0%B8%D1%81%D0%BA%D0%BE%D0%BF%D0%B0-%D0%B5%D0%BB%D1%86-%D1%80%D0%BE%D1%81%D1%81%D0%B8%D0%B8-%D0%B4%D0%B8%D1%82%D1%80%D0%B8%D1%85%D0%B0-%D0%B1%D1%80%D0%B0%D1%83%D1%8D%D1%80%D0%B0/";
        String expected = "https://www.lutherancathedral.ru/2022/04/16/пасхальное-приветствие-архиепископа-елц-россии-дитриха-брауэра/";
        String actual = crawler.decodeLink(input);
        
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("decodeLink - нормальная url")
    public void testDecodeLinkNormal() {
        String input = "https://www.lutherancathedral.ru/english/";
        String expected = "https://www.lutherancathedral.ru/english/";
        String actual = crawler.decodeLink(input);
        
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("containsForbiddenComponents - простой тест, true")
    public void testContainsForbiddenComponentsTrue() {
        Mockito.when(commonContext.getFORBIDDEN_COMPONENTS()).thenReturn(new ArrayList<>() {{
            add("mailto:");
        }});

        assertTrue(crawler.containsForbiddenComponents("mailto:me@test.test"));
    }

    @Test
    @DisplayName("containsForbiddenComponents - простой тест, false")
    public void testContainsForbiddenComponentsFalse() {
        Mockito.when(commonContext.getFORBIDDEN_COMPONENTS()).thenReturn(new ArrayList<>() {{
            add("нельзя");
        }});

        assertFalse(crawler.containsForbiddenComponents("mailto:me@test.test"));
    }
    
    @Test
    @DisplayName("filterLinks - не добавлять - есть в посещённых")
    public void testFilterLinksInVisited() {
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>() {{
            add("http://test.test/1");
        }});

        Set<String> expected = new HashSet<>();
        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> actual = crawler.filterLinks(input);
        
        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - не добавлять - не внутренняя")
    public void testFilterLinksNotInternal() {
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>());
        Set<String> expected = new HashSet<>();

        List<String> input = new ArrayList<>(){{
            add("http://not.test/1");
        }};
        Set<String> actual = crawler.filterLinks(input);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - не добавлять - содержит запрещёнку")
    public void testFilterLinksContainsForbidden() {
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>());
        Mockito.when(commonContext.getFORBIDDEN_COMPONENTS()).thenReturn(new ArrayList<>(){{add("1");}});

        Set<String> expected = new HashSet<>();
        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> actual = crawler.filterLinks(input);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - не добавлять - заблокировано robots.txt")
    public void testFilterLinksBlockedInRobots() {
        Mockito.when(rules.isAllowed("http://test.test/1")).thenReturn(false);
        Mockito.when(crawlerContext.getRobotsRules()).thenReturn(rules);
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>());

        Set<String> expected = new HashSet<>();
        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> actual = crawler.filterLinks(input);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - не добавлять в отфильтрованные - добавить в посещённые")
    public void testFilterLinksAddToVisited() {

        CrawlerContext specificCrawlerContext = new CrawlerContext(site, null, 1, new HashSet<>(), new SimpleRobotRules());
        CrawlerService crawlerService = new CrawlerService(specificCrawlerContext, commonContext);
        Mockito.when(commonContext.getFORBIDDEN_COMPONENTS()).thenReturn(new ArrayList<>() {{
            add("1");
        }});


        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> expected = new HashSet<>(){{
            add("http://test.test");
            add("http://test.test/1");
        }};

        crawlerService.filterLinks(input);
        Set<String> actual = specificCrawlerContext.getVisitedPages();

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - добавить в отфильтрованные")
    public void testFilterLinksAddToFiltered() {
        Mockito.when(rules.isAllowed("http://test.test/1")).thenReturn(true);
        Mockito.when(crawlerContext.getRobotsRules()).thenReturn(rules);
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>());

        Set<String> expected = new HashSet<>(){{
            add("http://test.test/1");
        }};
        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> actual = crawler.filterLinks(input);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - добавить в отфильтрованные - не добавлять в посещённые на этом этапе")
    public void testFilterLinksAddToFilteredDontAddToVisited() {

        CrawlerContext specificCrawlerContext = new CrawlerContext(site, null, 1, new HashSet<>(), new SimpleRobotRules());
        CrawlerService crawlerService = new CrawlerService(specificCrawlerContext, commonContext);

        List<String> input = new ArrayList<>(){{
            add("http://test.test/1");
        }};
        Set<String> expected = new HashSet<>(){{
            add("http://test.test");
        }};

        crawlerService.filterLinks(input);
        Set<String> actual = specificCrawlerContext.getVisitedPages();

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("getPageFromResponse - получить страницу из response")
    public void testGetPageFromResponse() throws IOException {
        String htmlFileAsString = new String(Files.readAllBytes(Path.of("src/test/resources/testPageCreation.html")));
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(htmlFileAsString);

        Page expected = new Page();
        expected.setSite(site);
        expected.setPath("/");
        expected.setCode(200);
        expected.setContent(htmlFileAsString);

        Page actual = crawler.getPageFromResponse(response);

        assertEquals(expected, actual);
    }

    @Nested
    @DisplayName("тестирование подсчёта лемм")
    class LemmaRankingTest {

        Set<Field> fields;
        MorphologyService morphologyService;

        @BeforeEach
        public void init() throws IOException {

            Field title = new Field();
            title.setName("title");
            title.setSelector("title");
            title.setWeight(1.);
            Field body = new Field();
            body.setName("body");
            body.setSelector("body");
            body.setWeight(0.8);
            fields = new HashSet<>(){{
                add(title);
                add(body);
            }};

            RussianLuceneMorphology rlm = new RussianLuceneMorphology();
            EnglishLuceneMorphology elm = new EnglishLuceneMorphology();
            String notAWord = "(?:\\.*\\s+\\-\\s+\\.*)|[^\\-а-яА-Яa-zA-Z\\d\\ё\\Ё]+";
            morphologyService = new MorphologyService(notAWord,rlm,elm);
        }

        @Test
        @DisplayName("getAndRankAllLemmas - одна лемма в title")
        public void testGetAndRankAllLemmasOneTitle() throws IOException {
            String html = "<html><head><title>title!</title></head><body></body></html>";
            Document doc = Jsoup.parse(html);

            Mockito.when(crawlerContext.getFields()).thenReturn(fields);
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);

            Lemma expectedLemma = new Lemma();
            expectedLemma.setSite(site);
            expectedLemma.setLemma("title");
            expectedLemma.setFrequency(1);
            expectedLemma.setRank(1.0);
            List<Lemma> expected = new ArrayList<>(){{
                add(expectedLemma);
            }};

            List<Lemma> actual = crawler.getAndRankAllLemmas(doc);

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
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);

            Lemma expectedLemma = new Lemma();
            expectedLemma.setSite(site);
            expectedLemma.setLemma("body");
            expectedLemma.setFrequency(1);
            expectedLemma.setRank(0.8);
            List<Lemma> expected = new ArrayList<>(){{
                add(expectedLemma);
            }};

            List<Lemma> actual = crawler.getAndRankAllLemmas(doc);

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
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);

            Lemma expectedLemma = new Lemma();
            expectedLemma.setSite(site);
            expectedLemma.setLemma("parse");
            expectedLemma.setFrequency(2);
            expectedLemma.setRank(1.8);
            List<Lemma> expected = new ArrayList<>(){{
                add(expectedLemma);
            }};

            List<Lemma> actual = crawler.getAndRankAllLemmas(doc);

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
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);

            List<Lemma> expected = new ArrayList<>();

            List<Lemma> actual = crawler.getAndRankAllLemmas(doc);

            assertIterableEquals(expected, actual);
        }

        @Test
        @DisplayName("getAndRankAllLemmas - две леммы, одна везде, вторая в теле")
        public void testGetAndRankAllLemmasTwoLemmas() throws IOException {
            String html = "<html><head><title>Title Title</title></head><body>Parsed parsed title!</body></html>";
            Document doc = Jsoup.parse(html);

            Mockito.when(crawlerContext.getFields()).thenReturn(fields);
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);

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

            List<Lemma> actual = crawler.getAndRankAllLemmas(doc);

            assertAll("Массивы должны быть одинаковыми по equals и у лемм должны совпадать частоты и ранги",
                    () -> assertIterableEquals(expected, actual, "массивы должны совпадать"),
                    () -> assertEquals(expected.get(0).getFrequency(),actual.get(0).getFrequency(), "частоты первых лемм должны быть равны"),
                    () -> assertEquals(expected.get(0).getRank(),actual.get(0).getRank(), "ранги первых лемм должны быть равны"),
                    () -> assertEquals(expected.get(1).getFrequency(),actual.get(1).getFrequency(), "частоты вторых лемм должны быть равны"),
                    () -> assertEquals(expected.get(1).getRank(),actual.get(1).getRank(), "ранги вторых лемм должны быть равны"));
        }
    }

    @Nested
    @DisplayName("Проверка обработки страницы со ссылками")
    class PageProcessingTest {
        String htmlFileAsString;
        CrawlerService crawlerSpy;
        List<String> forbidden;
        Set<String> visited;
        Document document;

        @BeforeEach
        public void init() throws IOException {
            htmlFileAsString = new String(Files.readAllBytes(Path.of("src/test/resources/testPageProcessing.html")));
            crawlerSpy = Mockito.spy(crawler);

            forbidden = new ArrayList<>(){{
                add("#");
                add("mailto:");
            }};

            visited = new HashSet<>(){{
                add("http://test.test/test1");
            }};

            document = Jsoup.parse(htmlFileAsString);
            document.setBaseUri("http://test.test/");
        }

        @Test
        @DisplayName("processOnePage - обработка одной страницы с разными ссылками")
        public void testProcessOnePageWithDifferentLinks() throws IOException {

            Mockito.when(crawlerContext.getVisitedPages()).thenReturn(visited);
            Mockito.doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            Mockito.when(response.contentType()).thenReturn("text/html");
            Mockito.when(response.statusCode()).thenReturn(200);
            Mockito.when(response.body()).thenReturn(htmlFileAsString);
            Mockito.when(response.parse()).thenReturn(document);
            Mockito.when(commonContext.isIndexing()).thenReturn(true);
            Mockito.when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));
            Mockito.when(commonContext.getDatabaseService()).thenReturn(databaseService);
            Mockito.when(commonContext.getFORBIDDEN_COMPONENTS()).thenReturn(forbidden);
            Mockito.when(crawlerContext.getRobotsRules()).thenReturn(rules);
            Mockito.when(rules.isAllowed(anyString())).thenReturn(true);

            Set<String> expected = new HashSet<>(){{
                add("http://test.test/correct-internal-link/");
                add("http://test.test/тоже-норм-ссылка");
                add("http://test.test/aiGeneratedCat.jpeg");
            }};

            Set<String> actual = crawlerSpy.processOnePage("http://test.test/test1");

            assertAll("метод должен вызвать filterLinks и ссылки должны правильно отфильтроваться",
                    () -> verify(crawlerSpy, times(1)).filterLinks(anyList()),
                    () -> assertIterableEquals(expected, actual));
        }

        @Test
        @DisplayName("processOnePage - mime type не какой-либо text")
        public void testProcessOnePageWithIncorrectMimeType() throws IOException {

            Mockito.doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            Mockito.when(response.contentType()).thenReturn("image/gif");

            Set<String> expected = new HashSet<>();

            Set<String> actual = crawlerSpy.processOnePage("http://test.test/test1");

            assertAll("Дальше проверки типа ответа метод выполняться не должен, набор ссылок должен быть пуст",
                    () -> verify(crawlerSpy, never()).getPageFromResponse(any()),
                    () -> assertIterableEquals(expected, actual));
        }

        @Test
        @DisplayName("processOnePage - больше не индексируем - горшочек не вари")
        public void testProcessOnePageNotIndexing() throws IOException {

            Mockito.doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            Mockito.when(response.contentType()).thenReturn("text/html");
            Mockito.when(response.statusCode()).thenReturn(200);
            Mockito.when(response.body()).thenReturn(htmlFileAsString);
            Mockito.when(response.parse()).thenReturn(document);
            Mockito.when(commonContext.isIndexing()).thenReturn(false);

            Set<String> expected = new HashSet<>();

            Set<String> actual = crawlerSpy.processOnePage("http://test.test/test1");

            assertAll("Не должно происходить обращения к бд, набор ссылок должен быть пуст",
                    () -> verify(commonContext, never()).getDatabaseService(),
                    () -> assertIterableEquals(expected, actual));
        }

        @Test
        @DisplayName("processOnePage - счётчик страниц кончился - горшочек не вари")
        public void testProcessOnePagePagesToIndexCounterIs0() throws IOException {

            Mockito.doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            Mockito.when(response.contentType()).thenReturn("text/html");
            Mockito.when(response.statusCode()).thenReturn(200);
            Mockito.when(response.body()).thenReturn(htmlFileAsString);
            Mockito.when(response.parse()).thenReturn(document);
            Mockito.when(commonContext.isIndexing()).thenReturn(true);
            Mockito.when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(0));

            Set<String> expected = new HashSet<>();

            Set<String> actual = crawlerSpy.processOnePage("http://test.test/test1");

            assertAll("Не должно происходить обращения к бд, набор ссылок должен быть пуст",
                    () -> verify(commonContext, never()).getDatabaseService(),
                    () -> assertIterableEquals(expected, actual));
        }

    }

    @Nested
    @DisplayName("Проверка compute")
    class ComputeTest {
        CrawlerService crawlerSpy;

        @Mock
        ForkJoinPool pool;
        @Mock
        Document document;
        @Mock
        DatabaseService databaseService;
        Random pseudoRandom;

        @BeforeEach
        public void init() {
            crawlerSpy = Mockito.spy(crawler);
            pseudoRandom = new Random(){
                @Override
                public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
                    return super.ints(0, 1);
                }
            };
        }

        @Test
        @DisplayName("compute - не индексируется - выход сразу")
        public void testComputeNotIndexing() {
            when(commonContext.isIndexing()).thenReturn(false);

            crawlerSpy.compute();

            verify(pool, never()).isShutdown();
        }

        @Test
        @DisplayName("compute - индексируется но остановлен - выход сразу")
        public void testComputeIsShutdown() {
            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(true);

            crawlerSpy.compute();

            verify(crawlerContext, never()).getNumberOfPagesToCrawl();
        }

        @Test
        @DisplayName("compute - счётчик закончился - выход сразу")
        public void testComputeCounterIs0() {
            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(0));

            crawlerSpy.compute();

            verify(crawlerSpy, never()).isSiteIndexedOrInterrupted(any());
        }

        @Test
        @DisplayName("compute - переиндексация? - индексацию быстро отменили")
        public void testComputeReindexButIndexingIsFalse() {
            when(commonContext.isIndexing())
                    .thenReturn(true)
                    .thenReturn(false);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));

            crawlerSpy.compute();

            verify(crawlerSpy, never()).isSiteIndexedOrInterrupted(any());
        }

        @Test
        @DisplayName("compute - переиндексация? - да, одна страница, без проблем")
        public void testComputeReindexYesOnePage() throws IOException {
            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));
            doReturn(true).when(crawlerSpy).isSiteIndexedOrInterrupted(any());
            when(crawlerContext.isReindexOnePage()).thenReturn(true);
            doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            when(response.contentType()).thenReturn("text");
            doReturn(new Page()).when(crawlerSpy).getPageFromResponse(response);
            when(response.parse()).thenReturn(document);
            doReturn(new ArrayList<Lemma>()).when(crawlerSpy).getAndRankAllLemmas(document);
            when(commonContext.getDatabaseService()).thenReturn(databaseService);

            crawlerSpy.compute();

            verify(databaseService, times(1)).reindexOnePage(any(), any(), anyList(), any());
        }

        @Test
        @DisplayName("compute - переиндексация? - да, одна страница, но ошибка на типе ответа")
        public void testComputeReindexYesOnePageButException() throws IOException {
            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));
            doReturn(true).when(crawlerSpy).isSiteIndexedOrInterrupted(any());
            when(crawlerContext.isReindexOnePage()).thenReturn(true);
            doReturn(response).when(crawlerSpy).getResponseFromLink(anyString());
            when(response.contentType()).thenReturn("image");


            IndexingStatusException exception = assertThrows(IndexingStatusException.class, () -> crawlerSpy.compute());
            assertEquals("Страницы с типом \"image\" не участвуют в индексировании", exception.getMessage());
        }

        @Test
        @DisplayName("compute - переиндексация? - да, весь сайт")
        public void testComputeReindexYesSite() throws IOException {

            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));
            doReturn(true).when(crawlerSpy).isSiteIndexedOrInterrupted(any());
            when(crawlerContext.isReindexOnePage()).thenReturn(false);
            when(commonContext.getDatabaseService()).thenReturn(databaseService);
            when(crawlerContext.getDelayGenerator()).thenReturn(pseudoRandom);
            doReturn(new HashSet<String>()).when(crawlerSpy).processOnePage(anyString());

            crawlerSpy.compute();

            InOrder inOrder = inOrder(databaseService, crawlerSpy);

            inOrder.verify(databaseService, times(1)).deleteSiteInformation(any());
            inOrder.verify(databaseService, times(1)).setSiteStatusToIndexing(any());
            inOrder.verify(crawlerSpy, times(1)).processOnePage(anyString());
        }

        @Test
        @DisplayName("compute - переиндексация? - нет")
        public void testComputeReindexNo() throws IOException {

            when(commonContext.isIndexing()).thenReturn(true);
            when(crawlerContext.getThisPool()).thenReturn(pool);
            when(pool.isShutdown()).thenReturn(false);
            when(crawlerContext.getNumberOfPagesToCrawl()).thenReturn(new AtomicInteger(100500));
            doReturn(false).when(crawlerSpy).isSiteIndexedOrInterrupted(any());
            when(crawlerContext.getDelayGenerator()).thenReturn(pseudoRandom);
            doReturn(new HashSet<String>()).when(crawlerSpy).processOnePage(anyString());

            crawlerSpy.compute();

            InOrder inOrder = inOrder(databaseService, crawlerSpy);

            inOrder.verify(databaseService, never()).deleteSiteInformation(any());
            inOrder.verify(databaseService, never()).setSiteStatusToIndexing(any());
            inOrder.verify(crawlerSpy, times(1)).processOnePage(anyString());
        }
    }


}

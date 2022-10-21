package com.github.dmitriims.posikengine.service.crawler;

import com.github.dmitriims.posikengine.model.*;
import com.github.dmitriims.posikengine.service.CommonContext;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.LemmaUtils;
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

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

    @Nested
    @DisplayName("Тесты на проверку года при фильтрации ссылок")
    class YearTest {
        int currentYear;

        @BeforeEach
        public void init() {
            currentYear = LocalDate.now().getYear();
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - нет параметров в строке")
        public void testIsYearInParametersAndAcceptableNoParameters() {
            assertTrue(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test.html"));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - нет года")
        public void testIsYearInParametersAndAcceptableNoYear() {
            assertTrue(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?PAGE=10"));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - есть год, вписывается в промежуток")
        public void testIsYearInParametersAndAcceptableYearNormal() {
            assertTrue(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?PAGE=10&year=" + currentYear));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - есть год, вписывается в промежуток, вариант 2")
        public void testIsYearInParametersAndAcceptableYearNormalVar2() {
            assertTrue(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?year=" + (currentYear - 1) + "&PAGE=10&"));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - есть год, меньше нижней границы")
        public void testIsYearInParametersAndAcceptableYearTooEarly() {
            assertFalse(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?PAGE=10&year=" + (currentYear - 11)));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - есть год, выше верхней границы")
        public void testIsYearInParametersAndAcceptableYearTooFarInTheFuture() {
            assertFalse(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?PAGE=10&year=" + (currentYear + 5)));
        }

        @Test
        @DisplayName("isYearInParametersAndAcceptable - есть год, формат странный")
        public void testIsYearInParametersAndAcceptableYearWithStrangeFormat() {
            assertFalse(crawler.ifYearIsPresentIsItInAcceptableRange("http://test.test/test?year=20&PAGE=10"));
        }
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
    @DisplayName("filterLinks - не добавлять - ссылка со слешем, но в посещённых без слеша")
    public void testFilterLinksSlashedInputButNoSlashInVisited() {
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>() {{
            add("http://test.test/1");
        }});

        Set<String> expected = new HashSet<>();
        List<String> input = new ArrayList<>(){{
            add("http://test.test/1/");
        }};
        Set<String> actual = crawler.filterLinks(input);

        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("filterLinks - не добавлять - ссылка без слеша, но в посещённых со слешем")
    public void testFilterLinksNonslashedInputButSlashInVisited() {
        Mockito.when(crawlerContext.getVisitedPages()).thenReturn(new HashSet<>() {{
            add("http://test.test/1/");
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
    }

}

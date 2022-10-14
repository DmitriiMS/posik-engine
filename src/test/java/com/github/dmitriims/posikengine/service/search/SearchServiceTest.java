package com.github.dmitriims.posikengine.service.search;

import com.github.dmitriims.posikengine.dto.PageDTO;
import com.github.dmitriims.posikengine.dto.SearchRequest;
import com.github.dmitriims.posikengine.exceptions.SearchException;
import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.service.CommonContext;
import com.github.dmitriims.posikengine.service.DatabaseService;
import com.github.dmitriims.posikengine.service.MorphologyService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    CommonContext commonContext;

    @Mock
    MorphologyService morphologyService;

    @Mock
    DatabaseService databaseService;

    SearchService searchService;

    @Getter
    @AllArgsConstructor
    class TestPageDTO implements PageDTO {
        String siteUrl;
        String siteName;
        String path;
        String content;
        double relevance;
    }


    @BeforeEach
    public void init() {
        searchService = new SearchService(commonContext);
    }

    @Test
    @DisplayName("lemmasContainAnyWordNormalForm - есть пересечение списков, true")
    public void testLemmasContainAnyWordNormalFormLemmasContainWordFromQuery() {

        List<String> wordNormalForms = new ArrayList<>() {{
            add("a");
            add("b");
            add("c");
        }};

        List<String> lemmas = new ArrayList<>() {{
            add("c");
            add("d");
            add("e");
        }};

        assertTrue(searchService.lemmasContainAnyWordNormalForm(wordNormalForms, lemmas));
    }

    @Test
    @DisplayName("lemmasContainAnyWordNormalForm - нет пересечения списков, false")
    public void testLemmasDontContainAnyWordNormalFormLemmasContainWordFromQuery() {

        List<String> wordNormalForms = new ArrayList<>() {{
            add("a");
            add("b");
            add("c");
        }};

        List<String> lemmas = new ArrayList<>() {{
            add("d");
            add("e");
            add("f");
        }};

        assertFalse(searchService.lemmasContainAnyWordNormalForm(wordNormalForms, lemmas));
    }

    @Test
    @DisplayName("correctQuery - убирает одно слово")
    public void testCorrectQueryRemoveOneWord() {
        Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);
        Mockito.when(morphologyService.splitStringToWords("Мама мыла раму"))
                .thenReturn(new String[]{"Мама", "мыла", "раму"});
        Mockito.when(morphologyService.getNormalFormOfAWord("мама"))
                .thenReturn(Collections.singletonList("мама"));
        Mockito.when(morphologyService.getNormalFormOfAWord("мыла"))
                .thenReturn(Collections.singletonList("мыла"));
        Mockito.when(morphologyService.getNormalFormOfAWord("раму"))
                .thenReturn(Collections.singletonList("раму"));

        String originalQuery = "Мама мыла раму";
        List<String> lemmas = new ArrayList<>() {{
            add("мама");
            add("мыла");
        }};
        String expected = "Мама мыла";
        String actual = searchService.correctQuery(lemmas, originalQuery);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("findRelevantPages - найдены страницы по набору лемм, леммы не удаляются")
    public void findRelevantPagesNormalSearch() {

        List<String> lemmas = new ArrayList<>() {{
            add("мама");
            add("мыла");
            add("раму");
        }};
        List<String> lemmasExpected = new ArrayList<>(lemmas);

        Site site = new Site();
        site.setId(1L);
        List<Site> sites = Collections.singletonList(site);

        PageDTO pageDTO = new TestPageDTO("test", "test", "test", "test", 4.2);
        List<PageDTO> found = Collections.singletonList(pageDTO);

        Mockito.when(commonContext.getDatabaseService()).thenReturn(databaseService);
        Mockito.when(databaseService.getSortedRelevantPageDTOs(anyList(), anyList(), anyInt(), anyInt()))
                .thenReturn(found);

        List<PageDTO> actual = searchService.findRelevantPages(lemmas, sites, 10, 0);

        assertAll("Должен быть возвращён список с одной страницей, список лемм должен остаться без изменений",
                () -> assertEquals(found, actual, "должна найтись одна страница"),
                () -> assertEquals(lemmasExpected, lemmas, "леммы должны остаться без изменений"));
    }

    @Test
    @DisplayName("findRelevantPages - найдены страницы по скорректированному списку лемм, одна лемма удалена")
    public void findRelevantPagesCorrectedSearch() {

        List<String> lemmas = new ArrayList<>() {{
            add("мама");
            add("мыла");
            add("раму");
        }};
        List<String> lemmasExpected = new ArrayList<>(lemmas);
        lemmasExpected.remove(0);

        Site site = new Site();
        site.setId(1L);
        List<Site> sites = Collections.singletonList(site);

        PageDTO pageDTO = new TestPageDTO("test", "test", "test", "test", 4.2);
        List<PageDTO> found = Collections.singletonList(pageDTO);

        Mockito.when(commonContext.getDatabaseService()).thenReturn(databaseService);
        Mockito.when(databaseService.getSortedRelevantPageDTOs(anyList(), anyList(), anyInt(), anyInt()))
                .thenReturn(new ArrayList<>())
                .thenReturn(found);

        List<PageDTO> actual = searchService.findRelevantPages(lemmas, sites, 10, 0);

        assertAll("Должен быть возвращён список с одной страницей, должна быть удалена первая лемма",
                () -> assertEquals(found, actual, "должна найтись одна страница"),
                () -> assertEquals(lemmasExpected, lemmas, "должна быть удалена первая лемма"));
    }

    @Nested
    @DisplayName("Проверка исключений")
    class searchExceptionTest {

        SearchService searchSpy;

        @BeforeEach
        public void init() {
            searchSpy = Mockito.spy(searchService);
        }


        @Test
        @DisplayName("search - searchWordsNormalForms size 0, exception")
        public void testSearchSearchWordsNormalFormsSize0() {
            List<Site> sites = Collections.singletonList(new Site());
            doReturn(sites).when(searchSpy).getSitesToSearch(anyString());
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);
            Mockito.when(morphologyService.getAndCountLemmasInString(anyString())).thenReturn(new HashMap<>());

            SearchException searchException = assertThrows(SearchException.class,
                    () -> searchSpy.search(new SearchRequest("test", "site", 0, 10)));
            assertEquals("Не удалось выделить леммы для поиска из запроса", searchException.getLocalizedMessage());
        }

        @Test
        @DisplayName("search - filteredLemmas size 0, exception")
        public void testSearchFilteredLemmasSize0() {
            List<Site> sites = Collections.singletonList(new Site());
            doReturn(sites).when(searchSpy).getSitesToSearch(anyString());
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);
            Mockito.when(morphologyService.getAndCountLemmasInString(anyString())).thenReturn(new HashMap<>(){{
                put("test", 1);
            }});
            Mockito.when(commonContext.getDatabaseService()).thenReturn(databaseService);
            Mockito.when(databaseService.filterPopularLemmasOut(anyList(),anyList(),anyDouble())).thenReturn(new ArrayList<>());

            SearchException searchException = assertThrows(SearchException.class,
                    () -> searchSpy.search(new SearchRequest("test", "site", 0, 10)));
            assertEquals("По запросу 'test' ничего не найдено", searchException.getLocalizedMessage());
        }

        @Test
        @DisplayName("search - foundPages size 0, exception")
        public void testSearchFoundPagesSize0() {
            List<Site> sites = Collections.singletonList(new Site());
            doReturn(sites).when(searchSpy).getSitesToSearch(anyString());
            Mockito.when(commonContext.getMorphologyService()).thenReturn(morphologyService);
            Mockito.when(morphologyService.getAndCountLemmasInString(anyString())).thenReturn(new HashMap<>(){{
                put("test", 1);
            }});
            Mockito.when(commonContext.getDatabaseService()).thenReturn(databaseService);
            Mockito.when(databaseService.filterPopularLemmasOut(anyList(),anyList(),anyDouble())).thenReturn(new ArrayList<>(){{
                add("test");
            }});
            doReturn(new ArrayList<PageDTO>()).when(searchSpy).findRelevantPages(anyList(),anyList(),anyInt(), anyInt());

            SearchException searchException = assertThrows(SearchException.class,
                    () -> searchSpy.search(new SearchRequest("test", "site", 0, 10)));
            assertEquals("По запросу 'test' ничего не найдено", searchException.getLocalizedMessage());
        }
    }


}

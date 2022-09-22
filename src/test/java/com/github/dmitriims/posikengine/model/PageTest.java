package com.github.dmitriims.posikengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PageTest {

    @Test
    @DisplayName("Одна и та же страница")
    public void testPageSame() {
        Page page = new Page();
        Page anotherPage = page;

        assertEquals(page, anotherPage);
    }

    @Test
    @DisplayName("разные адреса сайтов, не равны")
    public void testDiffSiteUrlsNotEqual() {
        Site site1 = new Site();
        site1.setUrl("http://test.test");
        site1.setName("test");

        Site site2 = new Site();
        site2.setUrl("http://not.test");
        site2.setName("test");

        Page page = new Page();
        page.setSite(site1);
        page.setCode(200);
        page.setLemmasHashcode(12345);
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site2);
        anotherPage.setCode(200);
        anotherPage.setLemmasHashcode(12345);
        anotherPage.setPath("path");

        assertNotEquals(page, anotherPage);
    }

    @Test
    @DisplayName("одинаковый сайт, разные хэши, не равны")
    public void testSameSiteDifferentHashcodeNotEqual() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(200);
        page.setLemmasHashcode(12345);
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(200);
        anotherPage.setLemmasHashcode(54321);
        anotherPage.setPath("path");

        assertNotEquals(page, anotherPage);
    }

    @Test
    @DisplayName("одинаковый сайт, одинаковый хэш, разные пути, не равны")
    public void testSameSiteSameHashDifferentPathNotEqual() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(200);
        page.setLemmasHashcode(12345);
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(200);
        anotherPage.setLemmasHashcode(12345);
        anotherPage.setPath("notPath");

        assertNotEquals(page, anotherPage);
    }

    @Test
    @DisplayName("Сайт Хэш и путь совпадают, равны")
    public void testSiteHashAndPathSameEqual() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(404);
        page.setLemmasHashcode(12345);
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(404);
        anotherPage.setLemmasHashcode(12345);
        anotherPage.setPath("path");

        assertEquals(page, anotherPage);
    }
}

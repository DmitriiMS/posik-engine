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
    @DisplayName("Код не 404, содержимое совпадает, один сайт, пути разные: равны")
    public void testPageNot404SameContentSameSiteDiffPathsEquals() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(200);
        page.setContent("content");
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(200);
        anotherPage.setContent("content");
        anotherPage.setPath("path/");

        assertEquals(page, anotherPage);
    }

    @Test
    @DisplayName("Код не 404, содержимое совпадает, разные сайты, пути одинаковые: не равны")
    public void testPageNot404SameContentDiffSitesSamePathEquals() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Site anotherSite = new Site();
        site.setUrl("http://not.test");
        site.setName("not");

        Page page = new Page();
        page.setSite(site);
        page.setCode(200);
        page.setContent("content");
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(anotherSite);
        anotherPage.setCode(200);
        anotherPage.setContent("content");
        anotherPage.setPath("path");

        assertNotEquals(page, anotherPage);
    }

    @Test
    @DisplayName("Код 404, содержимое совпадает, один сайт, пути разные: не равны")
    public void testPage404SameContentSameSiteDiffPathsEquals() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(404);
        page.setContent("content");
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(404);
        anotherPage.setContent("content");
        anotherPage.setPath("path/");

        assertNotEquals(page, anotherPage);
    }

    @Test
    @DisplayName("Код 404, содержимое совпадает, один сайт, пути совпадают: равны")
    public void testPage404SameContentSameSiteSamePathsEquals() {
        Site site = new Site();
        site.setUrl("http://test.test");
        site.setName("test");

        Page page = new Page();
        page.setSite(site);
        page.setCode(404);
        page.setContent("content");
        page.setPath("path");


        Page anotherPage = new Page();
        anotherPage.setSite(site);
        anotherPage.setCode(404);
        anotherPage.setContent("content");
        anotherPage.setPath("path");

        assertEquals(page, anotherPage);
    }
}

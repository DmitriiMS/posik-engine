package com.github.dmitriims.posikengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LemmaTest {

    @Test
    @DisplayName("Сайт и лемма совпадают - равны")
    public void testLemmaSiteAndLemmaSame() {

        Site site = new Site();
        site.setUrl("http://test.test");

        Lemma lemma = new Lemma();
        lemma.setSite(site);
        lemma.setLemma("lemma");
        lemma.setFrequency(100);
        lemma.setRank(500.);

        Lemma otherLemma = new Lemma();
        otherLemma.setSite(site);
        otherLemma.setLemma("lemma");
        otherLemma.setFrequency(100);
        otherLemma.setRank(500.);

        assertEquals(lemma, otherLemma);
    }

    @Test
    @DisplayName("Сайт и лемма совпадают частота и ранг разные - равны")
    public void testLemmaSiteAndLemmaSameFreqAndRankDiff() {

        Site site = new Site();
        site.setUrl("http://test.test");

        Lemma lemma = new Lemma();
        lemma.setSite(site);
        lemma.setLemma("lemma");
        lemma.setFrequency(101);
        lemma.setRank(503.);

        Lemma otherLemma = new Lemma();
        otherLemma.setSite(site);
        otherLemma.setLemma("lemma");
        otherLemma.setFrequency(102);
        otherLemma.setRank(504.);

        assertEquals(lemma, otherLemma);
    }

    @Test
    @DisplayName("Сайты разные, леммы совпадают - не равны")
    public void testLemmaSiteADiffLemmasSame() {

        Site site = new Site();
        site.setUrl("http://test.test");

        Lemma lemma = new Lemma();
        lemma.setSite(site);
        lemma.setLemma("lemma");

        Lemma otherLemma = new Lemma();
        otherLemma.setSite(new Site());
        otherLemma.setLemma("lemma");

        assertNotEquals(lemma, otherLemma);
    }

    @Test
    @DisplayName("Сайт один, леммы разные - не равны")
    public void testLemmaSiteASameLemmasDiff() {

        Site site = new Site();
        site.setUrl("http://test.test");

        Lemma lemma = new Lemma();
        lemma.setSite(site);
        lemma.setLemma("lemma");

        Lemma otherLemma = new Lemma();
        otherLemma.setSite(site);
        otherLemma.setLemma("not");

        assertNotEquals(lemma, otherLemma);
    }
}

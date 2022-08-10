package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    long countBySite(Site site);
    Lemma findByLemma(String lemma);
    void deleteAllBySite(Site site);


    @Query("select l from Lemma l " +
            "inner join Index i on i.lemma.id = l.id " +
            "where l.site in (:sites) " +
            "and l.lemma in (:lemmas)" +
            "group by l.id " +
            "having count(i.page.id) < (select cast(count(p.id) as double) * :threshold from Page p) " +
            "order by l.frequency asc")
    List<Lemma> filterVeryPopularLemmas(@Param("sites") List<Site> sites, @Param("lemmas") List<String> lemmas, @Param("threshold") double threshold);
}

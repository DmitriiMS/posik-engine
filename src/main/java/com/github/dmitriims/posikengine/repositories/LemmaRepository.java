package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.Tuple;
import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    long countBySite(Site site);

    @Query(value = "select * from lemma l where l.site_id=:siteId and l.lemma in :lemmas", nativeQuery = true)
    List<Tuple> findAllBySiteAndLemmaIn(Long siteId, List<String> lemmas);


    @Query(
            value = "select " +
                "distinct l.lemma, " +
                "sum(l.frequency) over (partition by l.lemma) as fr " +
            "from lemma l " +
            "join index i on l.id = i.lemma_id " +
            "where l.site_id in :siteIds " +
            "and l.lemma in :lemmas " +
            "group by l.lemma, l.frequency, l.id " +
            "having count(i.page_id) < (select cast(count(p.id) as double precision) * :threshold from page p) " +
            "order by fr asc",
            nativeQuery = true)
    List<Tuple> filterVeryPopularLemmas(
            List<Long> siteIds,
            List<String> lemmas,
            double threshold);
}

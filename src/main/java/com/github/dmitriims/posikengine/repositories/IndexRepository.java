package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Long> {
    List<Index> findAllByPage_Id(Long pageId);

    @Query(
            value = "select i.page_id " +
                    "from index i " +
                    "join lemma l on l.id = i.lemma_id " +
                    "where l.site_id in :sites " +
                    "and l.lemma = :lemma " +
                    "and i.page_id in (:pages)",
            nativeQuery = true
    )
    List<Long> findPageIdsBySiteInAndLemmaAndPageIdsIn(
            @Param("sites") List<Long> siteIds,
            @Param("lemma") String lemma,
            @Param("pages") List<Long> pageIds);
}


package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Page;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.Tuple;
import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);

    @Query(value = "select * from page where site_id=:siteId and path=:path", nativeQuery = true)
    Page findBySiteIdAndPagePath(Long siteId, String path);


    @Query(value = "select p.id from page p where p.site_id in :siteIds", nativeQuery = true)
    List<Long> getAllIdsBySiteId(List<Long> siteIds);

    @Query(
            value = "select distinct " +
                        "s.url as siteUrl, " +
                        "s.name as siteName, " +
                        "p.path, " +
                        "p.content, " +
                        "sum(i.rank) over (partition by p.path) as relevance " +
                    "from page p " +
                    "join index i on p.id = i.page_id " +
                    "join lemma l on l.id = i.lemma_id " +
                    "join site s on s.id = p.site_id " +
                    "where l.lemma in :lemmas " +
                    "and p.id in :pageIds " +
                    "order by relevance desc " +
                    "limit :limit " +
                    "offset :offset",
            nativeQuery = true
    )
    List<Tuple> getLimitedSortedPagesByLemmasAndPageIds(
            List<String> lemmas,
            List<Long> pageIds,
            int limit,
            int offset);
}

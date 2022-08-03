package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Page;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);

    @Query(nativeQuery = true, value = "select p.id from page p where p.site_id = ?1")
    List<Long> getAllIdsBySiteId(long site_id);

    void deleteAllBySite_Id(long site_id);
}

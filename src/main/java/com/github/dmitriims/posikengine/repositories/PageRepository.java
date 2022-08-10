package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Page;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySite(Site site);
    void deleteBySite(Site site);
    void deleteAllBySite(Site site);
    List<Page> findAllBySiteIn(List<Site> site);
    List<Page> findAllByIdIn(Collection<Long> ids);

    @Query(nativeQuery = true, value = "select p.id from page p where p.site_id = ?1")
    List<Long> getAllIdsBySiteId(long siteId);
}

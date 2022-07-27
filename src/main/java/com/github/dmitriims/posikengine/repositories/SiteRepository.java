package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Site;
import com.github.dmitriims.posikengine.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    boolean existsByUrl(String url);
    boolean existsByStatus(Status status);
}

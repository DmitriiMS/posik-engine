package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Lemma;
import com.github.dmitriims.posikengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    long countBySite(Site site);
}

package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, Long> {
}

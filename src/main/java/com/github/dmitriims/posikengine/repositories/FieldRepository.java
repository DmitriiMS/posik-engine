package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Field;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Long> {
    boolean existsByName(String name);
}

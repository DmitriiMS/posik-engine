package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Index;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexRepository extends JpaRepository<Index, Long> {
    Index findByPage_IdAndLemma_Id(long page_id, long lemma_id);
    void deleteAllByPage_Id(Long page_id);
}

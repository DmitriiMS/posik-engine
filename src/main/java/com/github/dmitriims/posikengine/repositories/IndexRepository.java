package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Index;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;

public interface IndexRepository extends JpaRepository<Index, Long> {
    Index findByPage_IdAndLemma_Id(long page_id, long lemma_id);
    void deleteAllByPage_Id(Long page_id);
    Set<Index> findAllByPage_IdInAndLemma_Id(Collection<Long> pageIds, Long lemmaId);
}

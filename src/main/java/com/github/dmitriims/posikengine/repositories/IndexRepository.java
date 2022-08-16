package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.Index;
import com.github.dmitriims.posikengine.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IndexRepository extends JpaRepository<Index, Long> {
    Index findByPage_IdAndLemma_Id(long pageId, long lemmaId);
    void deleteAllByPage_Id(Long pageId);
    Set<Index> findAllByPage_IdInAndLemma_Id(Collection<Long> pageIds, Long lemmaId);
    List<Index> findAllByPage_Id(Long pageId);
}


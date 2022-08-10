package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "`index`",
        uniqueConstraints = {
        @UniqueConstraint(name = "UniquePageAndLemma", columnNames = {"page_id", "lemma_id"})
})
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(name = "`rank`")
    private double rank;

    public Index(){

    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page +
                ", lemma=" + lemma +
                ", rank=" + rank +
                '}';
    }
}

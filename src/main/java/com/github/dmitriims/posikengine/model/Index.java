package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "`index`",
        uniqueConstraints = {
        @UniqueConstraint(name = "UniquePageAndLemma", columnNames = {"page_id", "lemma_id"})
})
public class Index {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "index_sequence"
    )
    @SequenceGenerator(
            name = "index_sequence",
            allocationSize = 100
    )
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id")
    private Page page;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(name = "`rank`")
    private double rank;

    @Column(name = "`count`")
    private int count;

    public Index(){

    }

    public Index(Page page, Lemma lemma, double rank, int count) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
        this.count = count;
    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page +
                ", lemma=" + lemma +
                ", rank=" + rank +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index)) return false;
        Index index = (Index) o;
        return Objects.equals(page, index.page) && Objects.equals(lemma, index.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, lemma);
    }
}

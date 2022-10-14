package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;

import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uniqueSiteAndLemma", columnNames = {"site_id", "lemma"})
       },
        indexes = @javax.persistence.Index(name="lemma_index", columnList = "lemma"))
public class Lemma {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "lemma_sequence"
    )
    @SequenceGenerator(
            name = "lemma_sequence",
            allocationSize = 100
    )
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id")
    private Site site;

    private String lemma;

    private int frequency;

    @Transient
    private double rank;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY)
    @Cascade(CascadeType.DELETE)
    private Set<Index> indices;

    public Lemma() {

    }

    public Lemma(Long id, Site site, String lemma, int frequency) {
        this.id = id;
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", site=" + site +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                ", rank =" + rank +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lemma)) return false;
        Lemma lemma1 = (Lemma) o;
        return Objects.equals(site, lemma1.site) && Objects.equals(lemma, lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, lemma);
    }
}

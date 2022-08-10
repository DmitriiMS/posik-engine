package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;

import java.util.Set;

@Setter
@Getter
@Entity
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(unique = true)
    private String lemma;

    private int frequency;

    @Transient
    private double rank;

    @OneToMany(mappedBy = "lemma")
    @Cascade(CascadeType.DELETE)
    private Set<Index> indices;

    public Lemma() {

    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", site=" + site +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}

package com.github.dmitriims.posikengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Entity
@Data
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

    @OneToMany(mappedBy = "lemma")
    private Set<Index> indices;
}

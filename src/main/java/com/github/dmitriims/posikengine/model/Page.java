package com.github.dmitriims.posikengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Entity
@Data
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "`path`", unique = true)
    private String path;

    private int code;

    private String content;

    @OneToMany(mappedBy = "page")
    private Set<Index> indices;
}

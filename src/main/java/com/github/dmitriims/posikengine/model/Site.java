package com.github.dmitriims.posikengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true)
    private String url;

    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time")
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @OneToMany(mappedBy = "site")
    private Set<Page> pages;

    @OneToMany(mappedBy = "site")
    private Set<Lemma> lemmas;
}

package com.github.dmitriims.posikengine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "`name`", unique = true)
    private String name;

    private String selector;

    private float weight;
}

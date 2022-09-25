package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Setter
@Getter
@Entity
public class Field {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "field_sequence"
    )
    @SequenceGenerator(
            name = "field_sequence"
    )
    private Long id;

    @Column(name = "`name`", unique = true)
    private String name;

    private String selector;

    private double weight;

    @Override
    public String toString() {
        return "Field{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", selector='" + selector + '\'' +
                ", weight=" + weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Field)) return false;
        Field field = (Field) o;
        return Objects.equals(selector, field.selector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector);
    }
}

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
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UniqueSiteAndPath", columnNames = {"site_id", "`path`"})
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "`path`")
    private String path;

    private int code;

    @Column(length = 1_000_000)
    private String content;

    @OneToMany(mappedBy = "page")
    @Cascade(CascadeType.DELETE)
    private Set<Index> indices;

    public Page() {
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", site=" + site +
                ", path='" + path + '\'' +
                ", code=" + code +
                '}';
    }
}

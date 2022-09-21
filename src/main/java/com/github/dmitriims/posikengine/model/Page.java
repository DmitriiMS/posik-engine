package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "UniqueSiteAndPath", columnNames = {"site_id", "`path`"})
})
public class Page {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "page_sequence"
    )
    @SequenceGenerator(
            name = "page_sequence",
            allocationSize = 100
    )
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "`path`")
    private String path;

    private int code;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "lemmas_hashcode")
    private int lemmasHashcode;

    @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page)) return false;
        Page page = (Page) o;
        if (!Objects.equals(site.getUrl(), page.site.getUrl())) return false;
        if (!Objects.equals(lemmasHashcode, page.lemmasHashcode)) return false;
        return Objects.equals(path, page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site.getUrl(), path, code, lemmasHashcode);
    }
}

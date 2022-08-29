package com.github.dmitriims.posikengine.model;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Setter
@Getter
@Entity
public class Site {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "site_sequence"
    )
    @SequenceGenerator(
            name = "site_sequence"
    )
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

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    @Cascade(CascadeType.DELETE)
    private Set<Page> pages;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    @Cascade(CascadeType.DELETE)
    private Set<Lemma> lemmas;

    public Site() {

    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Site)) return false;
        Site site = (Site) o;
        return Objects.equals(url, site.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}

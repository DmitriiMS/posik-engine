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

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Page)) return false;
        final Page other = (Page) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$content = this.getContent();
        final Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        if (this.code == 404 && other.getCode() == 404) {
            final Object this$path = this.getPath();
            final Object other$path = other.getPath();
            return this$path == null ? other$path == null : this$path.equals(other$path);
        }
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Page;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        return result;
    }
}

package posikengine;

import lombok.Data;

import java.util.List;

@Data
public class Page {
    private int id;
    private String path;
    private int code;
    private String content;
    private List<Lemma> lemmas;

    public Page(String path, int code, String content, List<Lemma> lemmas) {
        this.path = path;
        this.code = code;
        this.content = content;
        this.lemmas = lemmas;
    }

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

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        return result;
    }
}

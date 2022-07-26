package com.github.dmitriims.posikengine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Lemma {
    private int id;
    private String normalForm;
    private int count;
    private double rank;

    public Lemma(String normalForm, int count, double rank) {
        this.normalForm = normalForm;
        this.count = count;
        this.rank = rank;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Lemma)) return false;
        final Lemma other = (Lemma) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$normalForm = this.getNormalForm();
        final Object other$normalForm = other.getNormalForm();
        if (this$normalForm == null ? other$normalForm != null : !this$normalForm.equals(other$normalForm))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Lemma;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $normalForm = this.getNormalForm();
        result = result * PRIME + ($normalForm == null ? 43 : $normalForm.hashCode());
        return result;
    }
}

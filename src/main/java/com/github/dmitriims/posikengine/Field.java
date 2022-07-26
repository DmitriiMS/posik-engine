package com.github.dmitriims.posikengine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Field {
    private String name;
    private String selector;
    private double weight;
}

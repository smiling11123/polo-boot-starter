package com.polo.boot.validation.model;

import com.polo.boot.validation.annotation.InputContent;

public record DynamicWordEntry(
        String word,
        String category,
        int level
) {
}

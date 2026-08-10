package com.verse.store.domain;

public record Verse(
        Long id,
        String book,
        int chapter,
        int number,
        String text
) {
}
package com.javel;

import java.util.List;

public class History {

    private final List<String> entries;

    private History(List<String> entries) {
        this.entries = entries;
    }

    public static History of(String... entries) {
        return new History(List.of(entries));
    }

    public List<String> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
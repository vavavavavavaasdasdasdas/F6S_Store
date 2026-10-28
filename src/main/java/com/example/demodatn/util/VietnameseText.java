package com.example.demodatn.util;

import java.text.Normalizer;
import java.util.Locale;

public final class VietnameseText {
    private VietnameseText() {}

    public static String clean(String input) {
        return input == null ? "" : input.trim().replaceAll("\\s+", " ");
    }

    public static String fold(String input) {
        String cleaned = clean(input);
        if (cleaned.isEmpty()) return "";
        String decomposed = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'd');
        return decomposed.toLowerCase(Locale.ROOT);
    }

    public static boolean sameFolded(String a, String b) {
        String fa = fold(a);
        return !fa.isEmpty() && fa.equals(fold(b));
    }
}

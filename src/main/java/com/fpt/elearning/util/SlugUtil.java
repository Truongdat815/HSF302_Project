package com.fpt.elearning.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sinh slug tu chuoi tieng Viet (bo dau, thuong hoa, thay khoang trang bang dau -).
 */
public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGE_DASH = Pattern.compile("(^-+|-+$)");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "item";
        }
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')   // d co gach (đ)
                .replace('Đ', 'D');
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = EDGE_DASH.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ROOT);
        return slug.isBlank() ? "item" : slug;
    }
}

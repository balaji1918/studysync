package com.studysync.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Turns "Java, sql ,  Java" into a clean list ["Java", "sql"]. Used for subjects and skills. */
public final class TagParser {

    private TagParser() { }

    /** Lower-case, trimmed, single-spaced form used to compare names. */
    public static String key(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * @throws IllegalArgumentException with a user-friendly message if an item is too long
     *                                  or there are too many items. Nothing is silently dropped.
     */
    public static List<String> parse(String raw, int maxItems, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String part : raw.split(",")) {
            String cleaned = part.trim().replaceAll("\\s+", " ");
            if (cleaned.isEmpty()) {
                continue;
            }
            if (cleaned.length() > maxLength) {
                throw new IllegalArgumentException("Each item can be at most " + maxLength + " characters long.");
            }
            unique.putIfAbsent(key(cleaned), cleaned);
        }
        if (unique.size() > maxItems) {
            throw new IllegalArgumentException("Please list at most " + maxItems + " items, separated by commas.");
        }
        return new ArrayList<>(unique.values());
    }
}

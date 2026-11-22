package com.example.animalpictures.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** The animals the service knows how to fetch pictures for. */
public enum AnimalType {
    CAT,
    DOG,
    BEAR;

    /** Case-insensitive lookup, e.g. "Cat", "cat" and "CAT" all resolve to {@link #CAT}. */
    public static AnimalType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Animal type must not be empty. Allowed values: " + allowedValues());
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown animal type '" + value + "'. Allowed values: " + allowedValues());
        }
    }

    public static String allowedValues() {
        return Arrays.stream(values()).map(AnimalType::value).collect(Collectors.joining(", "));
    }

    /** Lower-case representation used in URLs and JSON. */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}

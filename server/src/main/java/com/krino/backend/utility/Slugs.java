package com.krino.backend.utility;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEADING_DASHES = Pattern.compile("^-+");
    private static final Pattern TRAILING_DASHES = Pattern.compile("-+$");

    private Slugs() {}

    public static String slugify(String input) {
        Objects.requireNonNull(input, "input");

        String slug = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .replace("+", " plus ")
                .replace("#", " sharp ");

        slug = slug
                .replace("ß", "ss")
                .replace("æ", "ae")
                .replace("œ", "oe")
                .replace("ø", "o")
                .replace("ł", "l")
                .replace("đ", "d")
                .replace("ð", "d")
                .replace("þ", "th")
                .replace("ħ", "h")
                .replace("ı", "i");

        slug = COMBINING_MARKS.matcher(slug).replaceAll("");
        slug = NON_ALPHANUMERIC.matcher(slug).replaceAll("-");
        slug = LEADING_DASHES.matcher(slug).replaceAll("");
        slug = TRAILING_DASHES.matcher(slug).replaceAll("");
        if (slug.isEmpty()) throw new IllegalArgumentException("Text must include searchable characters.");

        return slug;
    }
}

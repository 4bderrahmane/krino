package com.krino.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordGenerator {

    private final SecureRandom secureRandom;

    // these ambiguous characters are intentionally removed: l, I, O, 0, 1
    private static final String LOWER   = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS  = "23456789";
    private static final String SYMBOLS = "!@#$%^&*";
    private static final String ALL     = LOWER + UPPER + DIGITS + SYMBOLS;

    private static final int LENGTH      = 16;
    private static final int MIN_LOWER   = 1;
    private static final int MIN_UPPER   = 1;
    private static final int MIN_DIGITS  = 2;
    private static final int MIN_SYMBOLS = 1;


    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);

        appendFrom(chars, LOWER, MIN_LOWER);
        appendFrom(chars, UPPER, MIN_UPPER);
        appendFrom(chars, DIGITS, MIN_DIGITS);
        appendFrom(chars, SYMBOLS, MIN_SYMBOLS);
        appendFrom(chars, ALL, LENGTH - chars.size());

        Collections.shuffle(chars, secureRandom);

        StringBuilder sb = new StringBuilder(LENGTH);
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private void appendFrom(List<Character> target, String alphabet, int count) {
        for (int i = 0; i < count; i++) {
            target.add(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
    }
}

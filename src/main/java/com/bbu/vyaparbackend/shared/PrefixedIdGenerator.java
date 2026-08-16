package com.bbu.vyaparbackend.shared;

import java.security.SecureRandom;

public final class PrefixedIdGenerator {
    public static final int RANDOM_PART_LENGTH = 20;
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PrefixedIdGenerator() {
    }

    public static String generate(String prefix) {
        if (prefix == null || prefix.isBlank() || prefix.indexOf('_') >= 0) {
            throw new IllegalArgumentException("ID prefix must be non-blank and must not contain an underscore");
        }
        return prefix + "_" + randomPart(RANDOM_PART_LENGTH);
    }

    public static String randomPart(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Random value length must be positive");
        }
        StringBuilder value = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            value.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return value.toString();
    }
}

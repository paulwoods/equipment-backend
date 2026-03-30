package com.mrpaulwoods.equipment.backend.util;

import java.security.SecureRandom;
import java.util.stream.Collectors;

public final class IdGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String generate() {
        return RANDOM.ints(7, 0, CHARS.length())
                .mapToObj(i -> String.valueOf(CHARS.charAt(i)))
                .collect(Collectors.joining());
    }
}

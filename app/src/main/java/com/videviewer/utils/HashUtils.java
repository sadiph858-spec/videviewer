package com.videviewer.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtils - Cryptographic utilities for PIN/password hashing
 */
public final class HashUtils {

    private HashUtils() {}

    /**
     * SHA-256 hash a string, returns hex string
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on Android
            return input;
        }
    }

    /**
     * Simple pattern key generation from point sequence
     * e.g., [0,1,4,3] → "0-1-4-3"
     */
    public static String patternToKey(int[] points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.length; i++) {
            if (i > 0) sb.append("-");
            sb.append(points[i]);
        }
        return sb.toString();
    }
}

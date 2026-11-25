package com.kelompok8.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {}

    /**
     * Menormalkan nomor telepon Indonesia agar selalu konsisten diawali angka '0'.
     * Contoh:
     * - "+6281234567890" -> "081234567890"
     * - "6281234567890"  -> "081234567890"
     * - "0812-3456-7890" -> "081234567890"
     * - "81234567890"    -> "081234567890"
     */
    public static String normalizeToLocalZero(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-().]", "");
        if (cleaned.isEmpty()) {
            return "";
        }
        if (cleaned.startsWith("+62")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("62")) {
            cleaned = "0" + cleaned.substring(2);
        } else if (cleaned.startsWith("8") && cleaned.matches("^\\d+$")) {
            cleaned = "0" + cleaned;
        }
        return cleaned;
    }

    /**
     * Menghasilkan semua variasi format nomor yang ekuivalen (0..., 62..., +62..., 8...)
     * untuk pengecekan duplikasi & pencarian data lama di database.
     */
    public static Set<String> buildEquivalentVariants(String rawPhone) {
        Set<String> variants = new LinkedHashSet<>();
        if (rawPhone == null || rawPhone.isBlank()) {
            return variants;
        }
        String trimmed = rawPhone.trim();
        String normalized = normalizeToLocalZero(trimmed);
        if (!normalized.isEmpty()) {
            variants.add(normalized);
            if (normalized.startsWith("0") && normalized.length() > 1) {
                String core = normalized.substring(1);
                variants.add("62" + core);
                variants.add("+62" + core);
                variants.add(core);
            }
        }
        variants.add(trimmed);
        return variants;
    }

    public static boolean isValidNormalizedPhone(String normalizedPhone) {
        return normalizedPhone != null && normalizedPhone.matches("^0\\d{8,14}$");
    }
}

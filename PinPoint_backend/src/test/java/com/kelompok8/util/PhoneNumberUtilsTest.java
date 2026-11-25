package com.kelompok8.util;

import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilsTest {

    @Test
    void normalizeToLocalZero_handlesPlus62And62AndSpaces() {
        assertEquals("081234567890", PhoneNumberUtils.normalizeToLocalZero("+6281234567890"));
        assertEquals("081234567890", PhoneNumberUtils.normalizeToLocalZero("6281234567890"));
        assertEquals("081234567890", PhoneNumberUtils.normalizeToLocalZero("0812-3456 7890"));
        assertEquals("081234567890", PhoneNumberUtils.normalizeToLocalZero("81234567890"));
        assertEquals("", PhoneNumberUtils.normalizeToLocalZero(null));
        assertEquals("", PhoneNumberUtils.normalizeToLocalZero("   "));
    }

    @Test
    void buildEquivalentVariants_returnsAllEquivalentFormats() {
        Set<String> variants = PhoneNumberUtils.buildEquivalentVariants("6281234567890");
        assertTrue(variants.contains("081234567890"));
        assertTrue(variants.contains("6281234567890"));
        assertTrue(variants.contains("+6281234567890"));
        assertTrue(variants.contains("81234567890"));
    }

    @Test
    void isValidNormalizedPhone_validatesCorrectly() {
        assertTrue(PhoneNumberUtils.isValidNormalizedPhone("081234567890"));
        assertFalse(PhoneNumberUtils.isValidNormalizedPhone("6281234567890"));
        assertFalse(PhoneNumberUtils.isValidNormalizedPhone("0812"));
        assertFalse(PhoneNumberUtils.isValidNormalizedPhone("0812abc67890"));
        assertFalse(PhoneNumberUtils.isValidNormalizedPhone(""));
    }
}

package com.example.yourphone

import com.example.yourphone.core.NormalizationUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NormalizationUtilTest {

    @Test
    fun testNormalize() {
        assertEquals("8325551234", NormalizationUtil.normalize("832-555-1234"))
        assertEquals("+18325551234", NormalizationUtil.normalize("+1 (832) 555-1234"))
        assertEquals("123", NormalizationUtil.normalize("123"))
        assertEquals("", NormalizationUtil.normalize(""))
        assertEquals("", NormalizationUtil.normalize(null))
    }

    @Test
    fun testNumbersMatch() {
        assertTrue(NormalizationUtil.numbersMatch("+1 (832) 555-1234", "832-555-1234"))
        assertTrue(NormalizationUtil.numbersMatch("18325551234", "+18325551234"))
        assertTrue(NormalizationUtil.numbersMatch("8325551234", "8325551234"))
        
        assertFalse(NormalizationUtil.numbersMatch("8325551234", "8325551235"))
        assertFalse(NormalizationUtil.numbersMatch("12345", "123456"))
    }
}

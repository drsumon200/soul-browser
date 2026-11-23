package com.soulbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OmniboxCalculatorTest {
    @Test
    fun evaluatesBasicMathExpressions() {
        assertEquals("42", OmniboxCalculator.evaluate("6 * 7"))
        assertEquals("2.5", OmniboxCalculator.evaluate("5 / 2"))
        assertEquals("15", OmniboxCalculator.evaluate("200 % 7.5"))
        assertNull(OmniboxCalculator.evaluate("10 / 0"))
        assertNull(OmniboxCalculator.evaluate("hello world"))
    }
}

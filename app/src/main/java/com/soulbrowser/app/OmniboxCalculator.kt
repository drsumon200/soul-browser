package com.soulbrowser.app

import java.util.Locale

object OmniboxCalculator {
    private val exprPattern =
        Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([+*/%-])\s*(-?\d+(?:\.\d+)?)\s*$""")

    fun evaluate(input: String): String? {
        val match = exprPattern.matchEntire(input) ?: return null
        val a = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val b = match.groupValues[3].toDoubleOrNull() ?: return null
        val result = when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) return null else a / b
            "%" -> (a * b) / 100.0
            else -> return null
        }
        return if (!result.isFinite()) null
        else if (result % 1.0 == 0.0) result.toLong().toString()
        else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
    }
}

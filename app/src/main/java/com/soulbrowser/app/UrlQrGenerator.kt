package com.soulbrowser.app
import java.security.MessageDigest
object UrlVisualHashMatrix {
    fun generateIdenticonMatrix(url: String, size: Int = 11): Array<BooleanArray> {
        require(size in 1..64) { "Matrix size must be between 1 and 64" }
        val bytes = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        val grid = Array(size) { BooleanArray(size) }
        val half = (size + 1) / 2
        var bitIndex = 0
        for (y in 0 until size) for (x in 0 until half) {
            val b = bytes[(bitIndex / 8) % bytes.size].toInt()
            val on = ((b ushr (bitIndex % 8)) and 1) == 1
            grid[y][x] = on
            grid[y][size - 1 - x] = on
            bitIndex++
        }
        return grid
    }
}

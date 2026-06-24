package com.jbcbros.qbitremote.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun zeroOrNegativeBytes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("0 B", formatBytes(-1))
    }

    @Test
    fun belowOneKiB() {
        assertEquals("512.00 B", formatBytes(512))
    }

    @Test
    fun kibBoundary() {
        assertEquals("1.00 KB", formatBytes(1024))
    }

    @Test
    fun mebibBoundary() {
        assertEquals("1.00 MB", formatBytes(1024 * 1024L))
    }

    @Test
    fun fractionalKiB() {
        assertEquals("1.50 KB", formatBytes(1536))
    }

    @Test
    fun usesLocaleUsDecimal() {
        // Locale.US -> dot as decimal separator (not comma), regardless of device locale.
        assertEquals("1.50 KB", formatBytes(1536))
    }

    @Test
    fun speedFormatting() {
        assertEquals("0 B/s", formatSpeed(0))
        assertEquals("1.0 KB/s", formatSpeed(1024))
        assertEquals("1.5 KB/s", formatSpeed(1536))
    }

    @Test
    fun customDecimals() {
        assertEquals("1.5 KB", formatBytes(1536, 1))
        assertEquals("1.500 KB", formatBytes(1536, 3))
    }
}

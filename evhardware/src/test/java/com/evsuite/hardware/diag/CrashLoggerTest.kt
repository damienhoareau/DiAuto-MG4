package com.evsuite.hardware.diag

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evsuite.hardware.AppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * An oversized crash report must lose its TAIL, not its head: the exception and its stack
 * trace sit at the top of the file, and they are the only lines that explain the crash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashLoggerTest {

    private val maxBytes = 64 * 1024

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an oversized report keeps the exception and the stack trace`() {
        val context = context()
        val boom = IllegalStateException("BOOM unique marker")
        // The volume comes from the AppLogger section, which is at the END of the report:
        // that is what must be sacrificed, not the exception preceding it.
        AppLogger.clear()
        repeat(100) { AppLogger.i("NOISE", "x".repeat(2_000)) }

        CrashLogger.write(context, "EVTest", Thread.currentThread(), boom)
        val written = CrashLogger.read(context) ?: error("no report written")

        assertTrue("the header was truncated", written.contains("EVTest crash report"))
        assertTrue("the exception was truncated", written.contains("BOOM unique marker"))
        assertTrue("the stack trace was truncated", written.contains("CrashLoggerTest"))
        assertTrue("the truncation is not reported", written.contains("report truncated"))
    }

    @Test
    fun `the cause chain is spelled out`() {
        val context = context()
        val root = IllegalArgumentException("ROOT unique marker")
        AppLogger.clear()

        CrashLogger.write(context, "EVTest", Thread.currentThread(), RuntimeException("wrapper", root))
        val written = CrashLogger.read(context) ?: error("no report written")

        assertTrue(written.contains("-- Caused by --"))
        assertTrue(written.contains("ROOT unique marker"))
    }

    @Test
    fun `read and clear track whether a report exists`() {
        val context = context()
        AppLogger.clear()
        CrashLogger.write(context, "EVTest", Thread.currentThread(), IllegalStateException("boom"))
        assertTrue(CrashLogger.hasReport(context))

        CrashLogger.clear(context)
        assertFalse(CrashLogger.hasReport(context))
        assertEquals(null, CrashLogger.read(context))
    }

    @Test
    fun `a normally sized report is left untouched`() {
        val content = "small report"
        assertEquals(content, String(CrashLogger.truncate(content), Charsets.UTF_8))
    }

    @Test
    fun `truncation respects the byte ceiling`() {
        // Accented characters are two bytes each in UTF-8. Counting characters instead
        // would write almost twice the announced ceiling.
        val content = "é".repeat(maxBytes)
        val truncated = CrashLogger.truncate(content)
        assertTrue("ceiling exceeded: ${truncated.size}", truncated.size <= maxBytes)
    }

    @Test
    fun `truncation keeps the beginning and not the end`() {
        val content = "HEAD_MARKER" + "x".repeat(maxBytes * 2) + "TAIL_MARKER"
        val truncated = String(CrashLogger.truncate(content), Charsets.UTF_8)
        assertTrue(truncated.startsWith("HEAD_MARKER"))
        assertFalse("the tail should have been cut", truncated.contains("TAIL_MARKER"))
    }

    @Test
    fun `truncation never cuts a multi-byte character in half`() {
        // Two and three bytes per character. Where the cut lands relative to a character
        // boundary depends on the marker's own length, so a single width can happen to
        // land cleanly and never exercise the backup loop; two widths cannot both.
        for (glyph in listOf("é", "€")) {
            val truncated = CrashLogger.truncate(glyph.repeat(maxBytes))
            assertFalse(
                "character cut in half for $glyph",
                String(truncated, Charsets.UTF_8).contains('�')
            )
            assertTrue("ceiling exceeded for $glyph: ${truncated.size}", truncated.size <= maxBytes)
        }
    }

    private fun errorWithFrames(frames: List<StackTraceElement>) =
        StackOverflowError("stack size 1009KB").apply { stackTrace = frames.toTypedArray() }

    private fun frame(method: String) = StackTraceElement("com.example.Service", method, "Service.kt", 218)

    @Test
    fun `a runaway recursion is folded into one line`() {
        val recursion = errorWithFrames(List(1024) { frame("getActiveDevice") })

        val text = CrashLogger.formatStackTrace(recursion)

        assertEquals("the repeated frame must appear once", 1, text.split("getActiveDevice").size - 1)
        assertTrue("the repeat count is lost", text.contains("repeats 1024 times"))
        assertTrue(text.startsWith("java.lang.StackOverflowError"))
    }

    @Test
    fun `a deep varied trace keeps its first frames and says how many were dropped`() {
        val deep = errorWithFrames(List(100) { frame("method$it") })

        val text = CrashLogger.formatStackTrace(deep)

        assertTrue(text.contains("method0"))
        assertTrue(text.contains("method39"))
        assertFalse("frame 40 should have been cut", text.contains("method40"))
        assertTrue(text.contains("60 more frames not shown (100 in total)"))
    }

    @Test
    fun `a short trace is written in full with no note`() {
        val short = errorWithFrames(listOf(frame("a"), frame("b")))

        val text = CrashLogger.formatStackTrace(short)

        assertTrue(text.contains("Service.a(") && text.contains("Service.b("))
        assertFalse(text.contains("not shown"))
        assertFalse(text.contains("repeats"))
    }

    @Test
    fun `the log section survives a runaway recursion`() {
        val context = context()
        AppLogger.clear()
        AppLogger.i("CONTEXT", "unique marker from before the crash")

        CrashLogger.write(context, "EVTest", Thread.currentThread(), errorWithFrames(List(1024) { frame("getActiveDevice") }))
        val written = CrashLogger.read(context) ?: error("no report written")

        assertTrue("the log section was cut off", written.contains("unique marker from before the crash"))
        assertFalse("the report was truncated", written.contains("report truncated"))
    }
}

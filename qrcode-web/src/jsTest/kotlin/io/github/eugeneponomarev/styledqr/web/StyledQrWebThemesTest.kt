package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.toSvg
import io.github.eugeneponomarev.styledqr.theme.QrThemes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StyledQrWebThemesTest {

    @Test
    fun exportsStableThemeIdentifiers() {
        assertContentEquals(
            arrayOf(
                "aurora",
                "fintech",
                "minimal",
                "neon",
                "wedding",
                "coffee",
                "cyber",
            ),
            getStyledQrThemeIds(),
        )
    }

    @Test
    fun themedExportMatchesTheSharedRenderer() {
        val content = "https://example.com"

        for (level in listOf(QrErrorCorrectionLevel.M, QrErrorCorrectionLevel.H)) {
            val code = QrCodeGenerator.encodeText(
                text = content,
                errorCorrection = level,
            )

            QrThemes.all.forEach { theme ->
                assertEquals(
                    code.toSvg(style = theme.createStyle()),
                    generateThemedQrSvg(
                        content = content,
                        themeId = theme.id.name.lowercase(),
                        errorCorrection = level.name,
                    ),
                    "${theme.id}: $level",
                )
            }
        }
    }

    @Test
    fun acceptsCaseInsensitiveTrimmedThemeIds() {
        assertEquals(
            generateThemedQrSvg("https://example.com", "fintech"),
            generateThemedQrSvg("https://example.com", " FINTECH "),
        )
    }

    @Test
    fun rejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> {
            generateThemedQrSvg("", "fintech")
        }
        assertFailsWith<IllegalArgumentException> {
            generateThemedQrSvg("https://example.com", "unknown")
        }
        assertFailsWith<IllegalArgumentException> {
            generateThemedQrSvg("https://example.com", "fintech", "unknown")
        }
    }
}
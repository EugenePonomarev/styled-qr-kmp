@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrCodeDecoder
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrDecodedPayload
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.SvgQrRenderer
import kotlin.js.JsExport
import kotlin.js.js
import kotlin.js.jsTypeOf

/** Encoding parameters, independent of visual styling and output dimensions. */
@JsExport
public class StyledQrEncodingOptions {
    public var errorCorrection: String = "H"
    public var minVersion: Int = 1
    public var maxVersion: Int = 40
}

/**
 * An immutable encoded QR matrix without a quiet zone.
 *
 * Coordinates are zero-based: row first, column second.
 * errorCorrection reports the actual level selected by the shared encoder.
 */
@JsExport
public class StyledQrMatrix internal constructor(
    private val code: QrCode,
) {
    public val version: Int get() = code.version
    public val errorCorrection: String get() = code.errorCorrection.name
    public val size: Int get() = code.size

    public operator fun get(row: Int, column: Int): Boolean =
        code[
            checkedWebInteger(row, "row"),
            checkedWebInteger(column, "column"),
        ]

    public fun isFunctionModule(row: Int, column: Int): Boolean =
        code.isFunctionModule(
            checkedWebInteger(row, "row"),
            checkedWebInteger(column, "column"),
        )

    public fun isLocatorModule(row: Int, column: Int): Boolean =
        code.isLocatorModule(
            checkedWebInteger(row, "row"),
            checkedWebInteger(column, "column"),
        )

    /** Returns independent JavaScript arrays containing boolean module values. */
    public fun copyModules(): Array<Array<Boolean>> {
        val modules = code.copyModules()

        return Array(modules.size) { row ->
            Array(modules[row].size) { column ->
                modules[row][column]
            }
        }
    }

    /**
     * Renders this matrix with explicit output dimensions.
     *
     * Only the visual fields of options are applied. Encoding parameters
     * cannot change an already encoded matrix.
     *
     * Shared style, geometry and logo-safety validation remain active.
     */
    public fun toSvg(
        output: StyledQrExportOptions,
        options: StyledQrOptions = StyledQrOptions(),
    ): String {
        val style = resolveStyledQrStyle(options)
        val rasterOptions = prepareStyledQrExport(output)
        val logoDataUri = options.logoDataUri?.let(::validatedWebLogoDataUri)

        return SvgQrRenderer.renderWithRasterOptions(
            qrCode = code,
            options = rasterOptions,
            style = style,
            logoDataUri = logoDataUri,
        )
    }
}

/** A decoded payload with the same public metadata as the shared decoder. */
@JsExport
public class StyledQrDecodedPayload internal constructor(
    private val payload: QrDecodedPayload,
) {
    public val version: Int get() = payload.version
    public val errorCorrection: String get() = payload.errorCorrection.name
    public val mask: Int get() = payload.mask
    public val correctedErrorCount: Int get() = payload.correctedErrorCount
    public val eciAssignment: Int? get() = payload.eciAssignment

    /** Returns an independent JavaScript number[] containing unsigned bytes. */
    public fun copyBytes(): Array<Int> {
        val bytes = payload.copyBytes()

        return Array(bytes.size) { index ->
            bytes[index].toInt() and 0xFF
        }
    }

    /** Returns text only when the matrix declares UTF-8 through ECI 26. */
    public fun utf8TextOrNull(): String? = payload.utf8TextOrNull()
}

/**
 * Encodes UTF-8 text with ECI 26.
 *
 * Empty text is valid at this low-level entry point.
 */
@JsExport
public fun encodeStyledQrText(
    content: String,
    options: StyledQrEncodingOptions = StyledQrEncodingOptions(),
): StyledQrMatrix {
    require(jsTypeOf(content) == "string") {
        "content must be a string"
    }

    return encodeWebMatrix(options) { level, minimum, maximum ->
        QrCodeGenerator.encodeText(
            text = content,
            errorCorrection = level,
            minVersion = minimum,
            maxVersion = maximum,
        )
    }
}

/**
 * Encodes unsigned bytes without adding an ECI designator.
 *
 * Input must be a JavaScript number[] with integer values in 0..255.
 * Convert a Uint8Array with Array.from(bytes).
 */
@JsExport
public fun encodeStyledQrBytes(
    bytes: Array<Int>,
    options: StyledQrEncodingOptions = StyledQrEncodingOptions(),
): StyledQrMatrix {
    requireCoreJsArray(bytes, "bytes")

    val count = checkedWebInteger(bytes.size, "bytes.length")
    val rawBytes = ByteArray(count) { index ->
        val value = checkedWebInteger(bytes[index], "bytes[$index]")

        require(value in 0..255) {
            "bytes[$index] must be in 0..255"
        }

        value.toByte()
    }

    return encodeWebMatrix(options) { level, minimum, maximum ->
        QrCodeGenerator.encodeBytes(
            bytes = rawBytes,
            errorCorrection = level,
            minVersion = minimum,
            maxVersion = maximum,
        )
    }
}

/**
 * Decodes a prepared boolean[][] matrix without a quiet zone.
 *
 * The side must be 21, 25, ..., 177 modules. Every row must have the same
 * length and every cell must be a boolean.
 *
 * Supports the Byte and ECI modes provided by the shared decoder.
 * This function does not recognize QR codes in photographs.
 */
@JsExport
public fun decodeStyledQrMatrix(
    modules: Array<Array<Boolean>>,
): StyledQrDecodedPayload {
    requireCoreJsArray(modules, "modules")

    val size = modules.size
    require(size in 21..177 && (size - 17) % 4 == 0) {
        "Matrix size must be one of 21, 25, ..., 177"
    }

    val prepared = Array(size) { row ->
        val sourceRow = modules[row]
        requireCoreJsArray(sourceRow, "modules[$row]")

        require(sourceRow.size == size) {
            "modules[$row] must contain $size cells"
        }

        BooleanArray(size) { column ->
            val value = sourceRow[column]

            require(jsTypeOf(value) == "boolean") {
                "modules[$row][$column] must be a boolean"
            }

            value
        }
    }

    return StyledQrDecodedPayload(QrCodeDecoder.decode(prepared))
}

private inline fun encodeWebMatrix(
    options: StyledQrEncodingOptions,
    encode: (QrErrorCorrectionLevel, Int, Int) -> QrCode,
): StyledQrMatrix {
    require(jsTypeOf(options.errorCorrection) == "string") {
        "errorCorrection must be a string"
    }

    val level = webEnum<QrErrorCorrectionLevel>(
        options.errorCorrection,
        "errorCorrection",
    )
    val minimum = checkedWebInteger(options.minVersion, "minVersion")
    val maximum = checkedWebInteger(options.maxVersion, "maxVersion")

    // Version bounds and payload capacity are validated by the shared encoder.
    return StyledQrMatrix(encode(level, minimum, maximum))
}

private fun requireCoreJsArray(value: Any?, name: String) {
    require(js("Array.isArray(value)") as Boolean) {
        "$name must be a JavaScript array"
    }
}

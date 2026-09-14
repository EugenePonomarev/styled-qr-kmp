package io.github.eugeneponomarev.styledqr.core

/**
 * An immutable QR matrix. Rows and columns are zero-based and do not include a quiet zone.
 */
public class QrCode internal constructor(
    public val version: Int,
    public val errorCorrection: QrErrorCorrectionLevel,
    private val modules: Array<BooleanArray>,
    private val functionModules: Array<BooleanArray>,
    private val locatorModules: Array<BooleanArray>,
    private val dataCodewordIndexes: Array<IntArray>,
    private val interleavedCodewordBlockIndexes: IntArray,
) {
    /** The width and height of the QR matrix in modules. */
    public val size: Int = modules.size

    /** Returns whether the module at [row], [column] is dark. */
    public operator fun get(row: Int, column: Int): Boolean {
        require(row in 0 until size) { "Row $row is outside 0..${size - 1}" }
        require(column in 0 until size) { "Column $column is outside 0..${size - 1}" }
        return modules[row][column]
    }

    /**
     * Returns `true` for finder, alignment, timing, format, version, and fixed dark modules.
     *
     * Renderers may use this marker to keep non-finder function modules square. Finder patterns are
     * rendered separately as composed eyes and use the configured finder-pattern shape.
     */
    public fun isFunctionModule(row: Int, column: Int): Boolean {
        require(row in 0 until size) { "Row $row is outside 0..${size - 1}" }
        require(column in 0 until size) { "Column $column is outside 0..${size - 1}" }
        return functionModules[row][column]
    }

    /**
     * Returns `true` for finder patterns, their separators, and alignment patterns.
     *
     * Unlike timing, format, and version bits, these patterns are used by a scanner to locate
     * and orient the QR code. Renderers can keep only this subset square while applying a custom
     * shape to the visible timing and format lines.
     */
    public fun isLocatorModule(row: Int, column: Int): Boolean {
        require(row in 0 until size) { "Row $row is outside 0..${size - 1}" }
        require(column in 0 until size) { "Column $column is outside 0..${size - 1}" }
        return locatorModules[row][column]
    }

    /** Returns a defensive copy of the QR matrix. */
    public fun copyModules(): Array<BooleanArray> = Array(size) { row -> modules[row].copyOf() }

    /**
     * Returns the index of the interleaved QR codeword stored at [row], [column], or `-1` when
     * the module is a function pattern or an unused remainder bit.
     *
     * This is internal encoder metadata used by renderers to calculate a safe logo exclusion
     * area. It is deliberately not part of the public matrix API.
     */
    internal fun dataCodewordIndexAt(row: Int, column: Int): Int = dataCodewordIndexes[row][column]

    /** Returns the Reed–Solomon block which owns an interleaved codeword. */
    internal fun blockIndexForInterleavedCodeword(codewordIndex: Int): Int =
        interleavedCodewordBlockIndexes[codewordIndex]
}

package io.github.eugeneponomarev.styledqr.core

/**
 * Content decoded from a QR matrix.
 *
 * The decoder preserves raw bytes because a Byte-mode QR code is not necessarily text.
 * [utf8TextOrNull] is available only when the QR declares UTF-8 through ECI assignment 26.
 */
public class QrDecodedPayload internal constructor(
    bytes: ByteArray,
    public val version: Int,
    public val errorCorrection: QrErrorCorrectionLevel,
    public val mask: Int,
    public val correctedErrorCount: Int,
    public val eciAssignment: Int?,
) {
    private val rawBytes: ByteArray = bytes.copyOf()

    init {
        require(version in 1..40) { "version must be in 1..40" }
        require(mask in 0..7) { "mask must be in 0..7" }
        require(correctedErrorCount >= 0) { "correctedErrorCount must not be negative" }
        require(eciAssignment == null || eciAssignment in 0..999_999) {
            "eciAssignment must be in 0..999999"
        }
    }

    /** Returns a defensive copy of the decoded Byte-mode payload. */
    public fun copyBytes(): ByteArray = rawBytes.copyOf()

    /** Returns text only when the QR explicitly declares UTF-8 through ECI assignment 26. */
    public fun utf8TextOrNull(): String? =
        if (eciAssignment == UTF8_ECI_ASSIGNMENT) rawBytes.decodeToString() else null

    private companion object {
        const val UTF8_ECI_ASSIGNMENT: Int = 26
    }
}

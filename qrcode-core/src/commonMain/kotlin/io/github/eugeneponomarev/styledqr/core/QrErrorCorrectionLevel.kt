package io.github.eugeneponomarev.styledqr.core

/**
 * QR error-correction levels, ordered from the smallest to the largest amount
 * of redundant data.
 */
public enum class QrErrorCorrectionLevel(
    internal val formatBits: Int,
) {
    L(formatBits = 1),
    M(formatBits = 0),
    Q(formatBits = 3),
    H(formatBits = 2),
}

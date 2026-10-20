package io.github.eugeneponomarev.styledqr.render

internal fun validateQrImageRenderArguments(
    sizePoints: Double,
    scale: Double,
) {
    requireImageParameter(sizePoints.isFinite(), "sizePoints must be finite")
    requireImageParameter(sizePoints > 0.0, "sizePoints must be greater than zero")
    requireImageParameter(scale.isFinite(), "scale must be finite")
    requireImageParameter(scale >= 0.0, "scale must be greater than or equal to zero")
}

internal fun qrImageTotalModules(
    matrixSize: Int,
    quietZoneModules: Int,
): Int {
    val total = matrixSize.toLong() + quietZoneModules.toLong() * 2L
    requireImageParameter(
        total in 1..Int.MAX_VALUE.toLong(),
        "QR image module count is outside the supported integer range",
    )
    return total.toInt()
}

internal fun validateQrImagePixelSide(
    sizePoints: Double,
    effectiveScale: Double,
) {
    requireImageParameter(
        effectiveScale.isFinite() && effectiveScale > 0.0,
        "Resolved image scale must be finite and greater than zero",
    )
    val pixels = sizePoints * effectiveScale
    requireImageParameter(
        pixels.isFinite() && pixels >= 1.0 && pixels <= Int.MAX_VALUE.toDouble(),
        "Image side in pixels must be finite and in 1..${Int.MAX_VALUE}",
    )
}

private fun requireImageParameter(condition: Boolean, message: String) {
    if (!condition) {
        throw QrRenderException(
            QrRenderErrorCode.InvalidRenderParameters,
            message,
        )
    }
}

package io.github.eugeneponomarev.styledqr.render

internal class QrRasterModuleLayout(
    val moduleSizePx: Int,
    val offsetPx: Int,
)

internal fun calculateRasterModuleLayout(
    sizePx: Int,
    totalModules: Int,
): QrRasterModuleLayout {
    require(sizePx >= totalModules) {
        "sizePx must be at least $totalModules to represent every QR module"
    }

    val moduleSizePx = sizePx / totalModules
    val qrSizePx = moduleSizePx * totalModules

    return QrRasterModuleLayout(
        moduleSizePx = moduleSizePx,
        offsetPx = (sizePx - qrSizePx) / 2,
    )
}

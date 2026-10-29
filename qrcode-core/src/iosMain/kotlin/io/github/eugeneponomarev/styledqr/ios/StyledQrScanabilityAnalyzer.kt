package io.github.eugeneponomarev.styledqr.ios

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRenderError
import io.github.eugeneponomarev.styledqr.render.QrScanabilityReport
import io.github.eugeneponomarev.styledqr.render.QrStyleConfiguration
import io.github.eugeneponomarev.styledqr.render.analyzeScanability

/** Analysis or a style-construction error. A report may itself contain validation errors. */
public class QrScanabilityAnalysisResult internal constructor(
    public val report: QrScanabilityReport?,
    public val error: QrRenderError?,
) {
    public val isSuccess: Boolean get() = report != null && error == null
}

/** Swift-friendly entry point for an already encoded matrix and mutable style configuration. */
public class StyledQrScanabilityAnalyzer {
    /**
     * The flag describes whether a logo will be drawn; logo pixels are not inspected. An
     * unsuccessful style build returns an error and no report or measurements. The report does not
     * guarantee image loading, successful platform rendering or optical readability.
     */
    public fun analyze(
        code: QrCode,
        options: QrStyleConfiguration,
        output: QrRasterRenderOptions?,
        logoIsRendered: Boolean,
    ): QrScanabilityAnalysisResult {
        val built = options.build()
        built.error?.let { return QrScanabilityAnalysisResult(null, it) }
        return QrScanabilityAnalysisResult(
            code.analyzeScanability(requireNotNull(built.style), output, logoIsRendered),
            null,
        )
    }
}

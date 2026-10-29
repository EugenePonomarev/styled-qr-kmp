@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.eugeneponomarev.styledqr.web

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.render.QrRasterRenderOptions
import io.github.eugeneponomarev.styledqr.render.QrRasterSizeMode
import io.github.eugeneponomarev.styledqr.render.QrRenderErrorCode
import io.github.eugeneponomarev.styledqr.render.QrScanabilityIssue
import io.github.eugeneponomarev.styledqr.render.QrScanabilityReport
import io.github.eugeneponomarev.styledqr.render.analyzeScanability
import kotlin.js.JsExport
import kotlin.js.jsTypeOf

/** Input for scanability analysis. The logo flag describes the intended render. */
@JsExport
public class StyledQrScanabilityOptions {
    public var style: StyledQrOptions = StyledQrOptions()
    public var output: StyledQrExportOptions? = null
    public var logoIsRendered: Boolean = false
}

@JsExport
public class StyledQrScanabilityIssue internal constructor(issue: QrScanabilityIssue) {
    public val code: String = issue.code.name
    public val message: String = issue.message
}

@JsExport
public class StyledQrScanabilityMeasurements internal constructor(report: QrScanabilityReport) {
    public val version: Int = report.measurements.version
    public val matrixModules: Int = report.measurements.matrixModules
    public val errorCorrection: String = report.measurements.errorCorrection.name
    public val quietZoneModules: Int = report.measurements.quietZoneModules
    public val pixelsPerModule: Double? = report.measurements.pixelsPerModule
    public val minContrastRatio: Double? = report.measurements.minContrastRatio
    public val contrastMargin: Double? = report.measurements.contrastMargin
    public val logoAffectedCodewordsInMostLoadedBlock: Int? = report.measurements.logoAffectedCodewordsInMostLoadedBlock
    public val logoAllowedCodewordsPerBlock: Int? = report.measurements.logoAllowedCodewordsPerBlock
    public val remainingLogoCodewordHeadroom: Int? = report.measurements.remainingLogoCodewordHeadroom
}

@JsExport
public class StyledQrScanabilityReport internal constructor(report: QrScanabilityReport) {
    public val errors: Array<StyledQrScanabilityIssue> = report.errors.map(::StyledQrScanabilityIssue).toTypedArray()
    public val warnings: Array<StyledQrScanabilityIssue> = report.warnings.map(::StyledQrScanabilityIssue).toTypedArray()
    public val measurements: StyledQrScanabilityMeasurements = StyledQrScanabilityMeasurements(report)
    public val isRenderable: Boolean = report.isRenderable
}

/** Exactly one of report and error is present for nonblank input; blank input is empty. */
@JsExport
public class StyledQrScanabilityResult internal constructor(
    public val report: StyledQrScanabilityReport?,
    public val error: StyledQrWebError?,
) {
    public val isSuccess: Boolean get() = report != null && error == null
    public val isEmpty: Boolean get() = report == null && error == null
}

/**
 * Analyzes text and options without rendering. Style and encoding options are validated before
 * blank content. The flag must reflect whether a logo will be drawn; logo image bytes are not
 * inspected. A report does not guarantee scanning or image decoding.
 */
@JsExport
public fun analyzeStyledQrScanability(
    content: String,
    options: StyledQrScanabilityOptions = StyledQrScanabilityOptions(),
): StyledQrScanabilityResult = try {
    val request = prepareStyledQrRequest(options.style)
    val output = options.output?.let(::prepareScanabilityOutput)
    if (content.isBlank()) {
        StyledQrScanabilityResult(null, null)
    } else {
        val code = try {
            QrCodeGenerator.encodeText(content, request.errorCorrection, request.minVersion, request.maxVersion)
        } catch (error: IllegalArgumentException) {
            throw StyledQrWebException(QrRenderErrorCode.PayloadTooLong, error.message ?: "Payload is too long", error)
        }
        val report = code.analyzeScanability(request.style, output, options.logoIsRendered)
        StyledQrScanabilityResult(StyledQrScanabilityReport(report), null)
    }
} catch (error: StyledQrWebException) {
    StyledQrScanabilityResult(null, StyledQrWebError(error.code.name, error.message ?: error.code.name))
} catch (error: Exception) {
    StyledQrScanabilityResult(null, StyledQrWebError(QrRenderErrorCode.RenderFailed.name, error.message ?: "Analysis failed"))
}

// Parse JS-only values here; shared analysis handles all numeric geometry validation.
private fun prepareScanabilityOutput(output: StyledQrExportOptions): QrRasterRenderOptions = try {
    val size = checkedWebInteger(output.sizePx, "sizePx")
    require(jsTypeOf(output.sizeMode) == "string") { "sizeMode must be a string" }
    QrRasterRenderOptions(size, webEnum<QrRasterSizeMode>(output.sizeMode, "sizeMode"))
} catch (error: IllegalArgumentException) {
    throw StyledQrWebException(QrRenderErrorCode.InvalidRenderParameters, error.message ?: "Invalid output", error)
}

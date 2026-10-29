package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCode
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel

/** Stable categories of checks performed by the scanability report. */
public enum class QrScanabilityIssueCode {
    LOW_CONTRAST,
    CONTRAST_INDETERMINATE_TRANSPARENCY,
    INVALID_RENDER_PARAMETERS,
    LOGO_AREA_DOES_NOT_FIT,
    LOGO_OVERLAPS_FUNCTION_PATTERN,
    LOGO_NO_CORRECTION_BUDGET,
    LOGO_EXCEEDS_RS_BUDGET,
}

/** A machine-readable category and a human-readable explanation. */
public class QrScanabilityIssue internal constructor(
    public val code: QrScanabilityIssueCode,
    public val message: String,
)

/** Measurements available from the checks; unavailable values are null. */
public class QrScanabilityMeasurements internal constructor(
    public val version: Int,
    public val matrixModules: Int,
    public val errorCorrection: QrErrorCorrectionLevel,
    public val quietZoneModules: Int,
    public val pixelsPerModule: Double?,
    public val minContrastRatio: Double?,
    public val contrastMargin: Double?,
    public val logoAffectedCodewordsInMostLoadedBlock: Int?,
    public val logoAllowedCodewordsPerBlock: Int?,
    public val remainingLogoCodewordHeadroom: Int?,
)

/**
 * Findings for the checks described in [analyzeScanability]. [isRenderable] only describes these
 * checks; it does not guarantee optical readability, image loading or successful allocation.
 */
public class QrScanabilityReport internal constructor(
    public val errors: List<QrScanabilityIssue>,
    public val warnings: List<QrScanabilityIssue>,
    public val measurements: QrScanabilityMeasurements,
) {
    public val isRenderable: Boolean get() = errors.isEmpty()
}

/**
 * Analyzes an encoded matrix and a valid style without rendering or changing either.
 *
 * If provided, raster geometry is checked first, followed by logo layout. Both errors may be
 * reported. Logo analysis runs only when [logoIsRendered] is true and [QrStyle.logo] exists.
 * With no raster options, pixel measurements are unavailable. Contrast covers QR foreground and
 * finder layers against the QR background, not the logo image or an opaque logo background.
 * A translucent rendered logo background makes contrast indeterminate. A passing report does not
 * guarantee that a QR code will scan, or account for image loading and platform rendering failures.
 */
public fun QrCode.analyzeScanability(
    style: QrStyle,
    rasterOptions: QrRasterRenderOptions? = null,
    logoIsRendered: Boolean = false,
): QrScanabilityReport {
    val errors = mutableListOf<QrScanabilityIssue>()
    val warnings = mutableListOf<QrScanabilityIssue>()

    val pixelsPerModule = if (rasterOptions == null) null else try {
        val total = qrImageTotalModules(size, style.quietZoneModules)
        calculateQrRasterGeometry(rasterOptions, total).moduleSizePx
    } catch (error: QrRenderException) {
        if (error.code != QrRenderErrorCode.InvalidRenderParameters) throw error
        errors += QrScanabilityIssue(QrScanabilityIssueCode.INVALID_RENDER_PARAMETERS, error.message.orEmpty())
        null
    }

    var affected: Int? = null
    var allowed: Int? = null
    val logoOptions = style.logo
    if (logoIsRendered && logoOptions != null) {
        when (val result = analyzeLogoLayout(logoOptions)) {
            is QrLogoAnalysisResult.Success -> {
                affected = result.affectedCodewordsInMostLoadedBlock
                allowed = result.allowedCodewordsPerBlock
            }
            is QrLogoAnalysisResult.Failure -> {
                affected = result.affectedCodewordsInMostLoadedBlock
                allowed = result.allowedCodewordsPerBlock
                val code = when (result.reason) {
                    QrLogoAnalysisFailure.AreaDoesNotFit -> QrScanabilityIssueCode.LOGO_AREA_DOES_NOT_FIT
                    QrLogoAnalysisFailure.OverlapsFunctionPattern -> QrScanabilityIssueCode.LOGO_OVERLAPS_FUNCTION_PATTERN
                    QrLogoAnalysisFailure.NoCorrectionBudget -> QrScanabilityIssueCode.LOGO_NO_CORRECTION_BUDGET
                    QrLogoAnalysisFailure.ExceedsCorrectionBudget -> QrScanabilityIssueCode.LOGO_EXCEEDS_RS_BUDGET
                }
                errors += QrScanabilityIssue(code, result.message)
            }
        }
    }

    val contrast = style.minimumRenderedContrast(logoIsRendered)
    if (contrast == null) {
        warnings += QrScanabilityIssue(
            QrScanabilityIssueCode.CONTRAST_INDETERMINATE_TRANSPARENCY,
            "Contrast cannot be determined without a backdrop for transparent layers.",
        )
    } else if (contrast < MIN_QR_FOREGROUND_CONTRAST_RATIO) {
        warnings += QrScanabilityIssue(
            QrScanabilityIssueCode.LOW_CONTRAST,
            "Minimum contrast $contrast:1 is below $MIN_QR_FOREGROUND_CONTRAST_RATIO:1.",
        )
    }

    return QrScanabilityReport(
        errors = errors.toList(),
        warnings = warnings.toList(),
        measurements = QrScanabilityMeasurements(
            version = version,
            matrixModules = size,
            errorCorrection = errorCorrection,
            quietZoneModules = style.quietZoneModules,
            pixelsPerModule = pixelsPerModule,
            minContrastRatio = contrast,
            contrastMargin = contrast?.minus(MIN_QR_FOREGROUND_CONTRAST_RATIO),
            logoAffectedCodewordsInMostLoadedBlock = affected,
            logoAllowedCodewordsPerBlock = allowed,
            remainingLogoCodewordHeadroom = if (affected != null && allowed != null) allowed - affected else null,
        ),
    )
}

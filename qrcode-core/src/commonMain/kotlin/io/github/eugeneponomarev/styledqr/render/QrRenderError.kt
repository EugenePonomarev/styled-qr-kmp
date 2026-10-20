package io.github.eugeneponomarev.styledqr.render

public enum class QrRenderErrorCode {
    InvalidStyle,
    InvalidRenderParameters,
    PayloadTooLong,
    LogoValidationFailed,
    RenderFailed,
}

public class QrRenderError internal constructor(
    public val code: QrRenderErrorCode,
    public val message: String,
)

internal class QrRenderException(
    val code: QrRenderErrorCode,
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

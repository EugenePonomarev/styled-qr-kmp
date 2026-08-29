package io.github.eugeneponomarev.styledqr.compose

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.eugeneponomarev.styledqr.android.StyledQrView
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrStyle

/**
 * Compose adapter for [StyledQrView]. It uses the same renderer and mutable API as XML/View
 * consumers, while Compose owns its placement and lifecycle.
 */
@Composable
public fun StyledQrCode(
    content: String,
    modifier: Modifier = Modifier,
    errorCorrection: QrErrorCorrectionLevel = QrErrorCorrectionLevel.H,
    style: QrStyle = QrStyle(),
    logo: Bitmap? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> StyledQrView(context) },
        update = { view ->
            view.setQrCode(
                content = content,
                errorCorrection = errorCorrection,
                style = style,
                logo = logo,
            )
        },
    )
}

package io.github.eugeneponomarev.styledqr.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import io.github.eugeneponomarev.styledqr.core.QrErrorCorrectionLevel
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import io.github.eugeneponomarev.styledqr.render.QrStyle
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyledQrViewRegressionTest {
    @Test
    fun drawHandlesOverflowAndRecoversAfterContentChange() {
        val view = StyledQrView(RuntimeEnvironment.getApplication()).apply {
            content = "x".repeat(10_000)
            errorCorrection = QrErrorCorrectionLevel.H
        }
        val spec = View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        view.layout(0, 0, 240, 240)
        val output = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            view.draw(canvas)
            assertContains(view.lastRenderError.orEmpty(), "Payload is too long")
            assertEquals(Color.WHITE, output.getPixel(120, 120))
            view.draw(canvas)
            assertContains(view.lastRenderError.orEmpty(), "Payload is too long")

            view.content = "https://example.com"
            view.draw(canvas)
            assertNull(view.lastRenderError)
            val expected = QrCodeGenerator.encodeText(view.content).toBitmap(240)
            try {
                assertTrue(output.sameAs(expected), "Valid content must restore the QR image")
            } finally {
                expected.recycle()
            }
        } finally {
            output.recycle()
        }
    }

    @Test
    fun omittedXmlStyleAttributesKeepSharedDefaultsExactly() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrContent, "https://example.com")
            .build()
        val view = StyledQrView(RuntimeEnvironment.getApplication(), attributes)
        assertEquals(QrStyle(), view.qrStyle)
    }

    @Test
    fun omittedXmlLogoFractionsKeepSharedDefaultsExactly() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrLogo, "@android:drawable/ic_menu_info_details")
            .build()
        val view = StyledQrView(RuntimeEnvironment.getApplication(), attributes)
        assertEquals(QrLogoOptions(), view.qrStyle.logo)
    }

    @Test
    fun explicitXmlFractionsRetainTypedArrayFloatPrecision() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrRoundedModuleRadiusFraction, "0.31")
            .build()
        val view = StyledQrView(RuntimeEnvironment.getApplication(), attributes)
        assertEquals(0.31f.toDouble(), view.qrStyle.roundedModuleRadiusFraction)
    }
}

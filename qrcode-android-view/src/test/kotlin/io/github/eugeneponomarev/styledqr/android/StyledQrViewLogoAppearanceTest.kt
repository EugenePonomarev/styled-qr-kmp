package io.github.eugeneponomarev.styledqr.android

import io.github.eugeneponomarev.styledqr.render.QrColor
import io.github.eugeneponomarev.styledqr.render.QrLogoBackgroundShape
import io.github.eugeneponomarev.styledqr.render.QrLogoOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StyledQrViewLogoAppearanceTest {
    @Test
    fun omittedLogoAppearanceKeepsWhiteDefaultsOnAColoredQrBackground() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrLogo, "@android:drawable/ic_menu_info_details")
            .addAttribute(R.attr.qrBackgroundColor, "#FFF0CC")
            .build()

        val view = StyledQrView(
            RuntimeEnvironment.getApplication(),
            attributes,
        )

        assertEquals(QrColor.fromHex("#FFF0CC"), view.qrStyle.background)
        assertEquals(QrLogoOptions(), view.qrStyle.logo)
    }

    @Test
    fun xmlSupportsEveryLogoBackgroundShapeAndIndependentColor() {
        val shapes = listOf(
            "square" to QrLogoBackgroundShape.Square,
            "roundedSquare" to QrLogoBackgroundShape.RoundedSquare,
            "circle" to QrLogoBackgroundShape.Circle,
        )

        for ((xmlValue, expectedShape) in shapes) {
            val attributes = Robolectric.buildAttributeSet()
                .addAttribute(
                    R.attr.qrLogo,
                    "@android:drawable/ic_menu_info_details",
                )
                .addAttribute(R.attr.qrBackgroundColor, "#FFF0CC")
                .addAttribute(R.attr.qrLogoBackgroundColor, "#DDEEFF")
                .addAttribute(R.attr.qrLogoBackgroundShape, xmlValue)
                .addAttribute(R.attr.qrLogoCornerRadiusFraction, "0.31")
                .build()

            val view = StyledQrView(
                RuntimeEnvironment.getApplication(),
                attributes,
            )

            assertEquals(
                QrLogoOptions(
                    background = QrColor.fromHex("#DDEEFF"),
                    backgroundShape = expectedShape,
                    cornerRadiusFraction = 0.31f.toDouble(),
                ),
                view.qrStyle.logo,
                xmlValue,
            )
        }
    }

    @Test
    fun xmlPreservesLogoBackgroundAlpha() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrLogo, "@android:drawable/ic_menu_info_details")
            .addAttribute(R.attr.qrLogoBackgroundColor, "#80445566")
            .build()

        val view = StyledQrView(
            RuntimeEnvironment.getApplication(),
            attributes,
        )

        assertEquals(
            QrColor(0x44, 0x55, 0x66, 0x80),
            view.qrStyle.logo?.background,
        )
    }

    @Test
    fun invalidRadiusUsesSharedLogoValidation() {
        for (radius in listOf("-0.01", "0.51")) {
            val attributes = Robolectric.buildAttributeSet()
                .addAttribute(
                    R.attr.qrLogo,
                    "@android:drawable/ic_menu_info_details",
                )
                .addAttribute(R.attr.qrLogoCornerRadiusFraction, radius)
                .build()

            assertFailsWith<IllegalArgumentException> {
                StyledQrView(
                    RuntimeEnvironment.getApplication(),
                    attributes,
                )
            }
        }
    }

    @Test
    fun omittedLogoKeepsTheExistingNullConfiguration() {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.qrBackgroundColor, "#FFF0CC")
            .build()

        val view = StyledQrView(
            RuntimeEnvironment.getApplication(),
            attributes,
        )

        assertNull(view.logo)
        assertNull(view.qrStyle.logo)
    }
}

package io.github.eugeneponomarev.styledqr.render

import io.github.eugeneponomarev.styledqr.core.QrCodeGenerator
import kotlin.test.Test

/** Opt-in exploratory JVM measurement, not a performance assertion or a calibrated benchmark. */
class QrScanabilityBenchmark {
    @Test
    fun measureReportCost() {
        if (System.getenv("QR_SCANABILITY_BENCHMARK") != "1") return
        val qr = QrCodeGenerator.encodeText("https://example.com")
        val styles = listOf(
            "solid" to QrStyle(),
            "gradient" to QrStyle(foregroundGradient = QrLinearGradient(QrColor.Black, QrColor(90, 70, 60))),
        )
        for ((name, style) in styles) {
            // Style construction and validation happen outside the measured region.
            repeat(200) { qr.analyzeScanability(style) }
            val samples = LongArray(500)
            var checksum = 0.0
            repeat(samples.size) { index ->
                val start = System.nanoTime()
                val report = qr.analyzeScanability(style)
                samples[index] = System.nanoTime() - start
                checksum += report.measurements.minContrastRatio ?: 0.0
            }
            samples.sort()
            println("scanability-benchmark $name: n=${samples.size}, " +
                "median=${samples[samples.size / 2]} ns, p95=${samples[samples.size * 95 / 100]} ns, " +
                "checksum=$checksum")
        }
    }
}

package io.github.eugeneponomarev.styledqr.core

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fixtures were generated once with an independent encoder,
 * not with this library.
 */
class QrReferenceCodewordsTest {
    @Test
    fun allVersionsMatchIndependentCodewordFixtures() {
        for (fixture in fixtures) {
            val digest = MessageDigest.getInstance("SHA-256")

            for (version in 1..40) {
                val capacity = fixture.capacities[version - 1]
                val headerBytes = if (version < 10) 2 else 3
                val payload = ByteArray(capacity - headerBytes) {
                    (33 + it % 90).toByte()
                }

                assertEquals(
                    capacity,
                    QrTables.numDataCodewords(version, fixture.level),
                    "Data capacity: version=$version, ECC=${fixture.level}",
                )

                val code = QrCodeGenerator.encodeBytes(
                    bytes = payload,
                    errorCorrection = fixture.level,
                    minVersion = version,
                    maxVersion = version,
                )
                val label = "version=$version, ECC=${fixture.level}"

                assertEquals(version, code.version, label)
                assertEquals(fixture.level, code.errorCorrection, label)

                val extracted =
                    QrMatrixReader(code.copyModules()).extractCodewords()

                digest.update(extracted.interleavedCodewords)
            }

            val actual = digest.digest().joinToString("") {
                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
            }

            assertEquals(
                fixture.sha256,
                actual,
                "ECC=${fixture.level}",
            )
        }
    }

    private data class Fixture(
        val level: QrErrorCorrectionLevel,
        val capacities: IntArray,
        val sha256: String,
    )

    private val fixtures = listOf(
        Fixture(
            QrErrorCorrectionLevel.L,
            intArrayOf(
                19, 34, 55, 80, 108, 136, 156, 194, 232, 274,
                324, 370, 428, 461, 523, 589, 647, 721, 795, 861,
                932, 1006, 1094, 1174, 1276, 1370, 1468, 1531, 1631, 1735,
                1843, 1955, 2071, 2191, 2306, 2434, 2566, 2702, 2812, 2956,
            ),
            "facd1a1f7bc8eb9587257c5e9c70a76fc803908ecaed11a55d618ee0190313d5",
        ),
        Fixture(
            QrErrorCorrectionLevel.M,
            intArrayOf(
                16, 28, 44, 64, 86, 108, 124, 154, 182, 216,
                254, 290, 334, 365, 415, 453, 507, 563, 627, 669,
                714, 782, 860, 914, 1000, 1062, 1128, 1193, 1267, 1373,
                1455, 1541, 1631, 1725, 1812, 1914, 1992, 2102, 2216, 2334,
            ),
            "0edfffc838d1bfc84b2c02d7e34fa82396a056296516e6b2575e23f3b8be21e9",
        ),
        Fixture(
            QrErrorCorrectionLevel.Q,
            intArrayOf(
                13, 22, 34, 48, 62, 76, 88, 110, 132, 154,
                180, 206, 244, 261, 295, 325, 367, 397, 445, 485,
                512, 568, 614, 664, 718, 754, 808, 871, 911, 985,
                1033, 1115, 1171, 1231, 1286, 1354, 1426, 1502, 1582, 1666,
            ),
            "0efb0e24bc8444b2b27ed79819cc7d819ab53e5f63621e7f12943d6fb9382a19",
        ),
        Fixture(
            QrErrorCorrectionLevel.H,
            intArrayOf(
                9, 16, 26, 36, 46, 60, 66, 86, 100, 122,
                140, 158, 180, 197, 223, 253, 283, 313, 341, 385,
                406, 442, 464, 514, 538, 596, 628, 661, 701, 745,
                793, 845, 901, 961, 986, 1054, 1096, 1142, 1222, 1276,
            ),
            "1d78f0a60753e3aa01fd2e40349ade7d75d51d80f6da42b6789757f3f4f5ff8a",
        ),
    )
}
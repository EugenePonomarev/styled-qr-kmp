package io.github.eugeneponomarev.styledqr.core

/** Decodes a finished QR module matrix without camera, image, or UI dependencies. */
public object QrCodeDecoder {

    /**
     * Decodes a square QR matrix without a quiet zone.
     *
     * The matrix may contain correctable module errors. This API currently supports the
     * Byte and ECI modes produced by [QrCodeGenerator].
     */
    public fun decode(
        modules: Array<BooleanArray>,
    ): QrDecodedPayload {
        val extracted = QrMatrixReader(modules).extractCodewords()
        val corrected = QrCodewordDecoder.correctAndExtractData(extracted)
        val payload = QrPayloadDecoder.decode(
            dataCodewords = corrected.dataCodewords,
            version = extracted.version,
        )

        return QrDecodedPayload(
            bytes = payload.bytes,
            version = extracted.version,
            errorCorrection = extracted.errorCorrection,
            mask = extracted.mask,
            correctedErrorCount = corrected.correctedErrorCount,
            eciAssignment = payload.eciAssignment,
        )
    }
}

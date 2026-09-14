package io.github.eugeneponomarev.styledqr.core

/** Thrown when a QR matrix is malformed or cannot be decoded. */
public class QrDecodeException(
    message: String,
) : IllegalArgumentException(message)

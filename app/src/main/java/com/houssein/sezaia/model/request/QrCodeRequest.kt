package com.houssein.sezaia.model.request

data class QrCodeRequest(
    val qr_code: String,
    val username: String,
    val role: String,
    val application_name: String
)

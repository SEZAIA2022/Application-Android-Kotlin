package com.houssein.sezaia.model.response

data class QrCodeResponse(
    val status: String,
    val message: String,
    val is_active: Boolean,
    val qrcodes: List <String>
)

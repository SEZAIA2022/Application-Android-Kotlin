package com.houssein.sezaia.model.request

data class AddQrRequest(
    val username: String,
    val location: String,
    val qr_code: String
)
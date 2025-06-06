package com.houssein.sezaia.model.response

data class ResendOtpResponse(
    val new_token: String,
    val message: String
)
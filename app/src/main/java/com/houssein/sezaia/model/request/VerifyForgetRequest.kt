package com.houssein.sezaia.model.request

data class VerifyForgetRequest(
    val email: String,
    val otp: String,
    val application_name: String
)
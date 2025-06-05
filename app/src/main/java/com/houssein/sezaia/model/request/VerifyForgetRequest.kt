package com.houssein.sezaia.model.request

data class VerifyForgetRequest(
    val otp: String,
    val token: String,
    val email: String
)
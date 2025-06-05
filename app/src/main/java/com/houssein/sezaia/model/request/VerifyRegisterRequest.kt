package com.houssein.sezaia.model.request

data class VerifyRegisterRequest(
    val otp: String,
    val token: String,
    val email: String
)
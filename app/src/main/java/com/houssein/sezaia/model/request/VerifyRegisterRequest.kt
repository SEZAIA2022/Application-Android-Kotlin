package com.houssein.sezaia.model.request

data class VerifyRegisterRequest(
    val email: String,
    val otp: String
)
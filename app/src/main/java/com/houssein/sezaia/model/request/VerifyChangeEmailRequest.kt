package com.houssein.sezaia.model.request

data class VerifyChangeEmailRequest(
    val otp: String,
    val token: String,
    val email: String
)
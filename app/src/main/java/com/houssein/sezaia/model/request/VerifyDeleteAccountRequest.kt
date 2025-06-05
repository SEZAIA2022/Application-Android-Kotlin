package com.houssein.sezaia.model.request

data class VerifyDeleteAccountRequest(
    val otp: String,
    val token: String,
    val email: String
)


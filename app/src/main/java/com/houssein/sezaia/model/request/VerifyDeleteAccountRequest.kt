package com.houssein.sezaia.model.request

data class VerifyDeleteAccountRequest(
    val otp: String,
    val email: String
)


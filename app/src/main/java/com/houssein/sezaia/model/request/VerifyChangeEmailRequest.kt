package com.houssein.sezaia.model.request

data class VerifyChangeEmailRequest(
    val email: String,       // Ancien email
    val otp: String
)
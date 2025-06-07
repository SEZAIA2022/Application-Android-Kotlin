package com.houssein.sezaia.model.request

data class VerifyChangeEmailRequest(
    val email: String,       // Ancien email
    val new_email: String,   // Nouvel email
    val otp: String
)
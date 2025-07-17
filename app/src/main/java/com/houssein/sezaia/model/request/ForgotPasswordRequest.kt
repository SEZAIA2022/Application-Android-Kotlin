package com.houssein.sezaia.model.request

data class ForgotPasswordRequest(
    val email: String,
    val application_name: String
)

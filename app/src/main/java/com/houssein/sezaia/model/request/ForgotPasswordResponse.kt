package com.houssein.sezaia.model.response

data class ForgotPasswordResponse(
    val message: String,
    val token: String?,
    val email: String?
)

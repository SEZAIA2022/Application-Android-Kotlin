package com.houssein.sezaia.model.response

data class LoginResponse(
    val message: String,
    val role: String,
    val user: String,
    val email: String
)
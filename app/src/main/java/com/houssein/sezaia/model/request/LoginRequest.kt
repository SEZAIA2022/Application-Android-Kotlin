package com.houssein.sezaia.model.request

data class LoginRequest(
    val username: String,
    val password: String,
    val application_name: String,
    val token: String? = null
)
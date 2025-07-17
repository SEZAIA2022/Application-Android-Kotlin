package com.houssein.sezaia.model.request

data class SignUpRequest(
    val username: String,
    val email: String,
    val password: String,
    val confirm_password: String,
    val number: String,
    val address: String,
    val country_code: String,
    val city: String,
    val postal_code: String,
    val application_name: String
)

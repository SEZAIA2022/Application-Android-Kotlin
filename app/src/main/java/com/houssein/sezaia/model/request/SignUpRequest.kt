package com.houssein.sezaia.model.request

data class SignUpRequest(
    val username: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val number: String,
    val address: String,
    val countryCode: String,
    val city: String,
    val postalCode: String
)

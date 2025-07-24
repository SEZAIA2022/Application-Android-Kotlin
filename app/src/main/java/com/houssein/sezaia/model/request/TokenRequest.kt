package com.houssein.sezaia.model.request

data class TokenRequest(
    val token: String,
    val username: String,
    val application_name : String
)

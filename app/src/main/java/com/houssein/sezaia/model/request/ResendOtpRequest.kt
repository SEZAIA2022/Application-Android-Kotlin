package com.houssein.sezaia.model.request

data class ResendOtpRequest(
    val email: String,
    val previous_page: String,
    val application_name: String
)


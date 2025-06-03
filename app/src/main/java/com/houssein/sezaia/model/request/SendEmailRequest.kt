package com.houssein.sezaia.model.request

data class SendEmailRequest(
    val to_email: String,
    val message: String
)

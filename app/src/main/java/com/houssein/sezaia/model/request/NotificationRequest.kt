package com.houssein.sezaia.model.request

data class NotificationRequest(
    val message: String,
    val role: String,
    val email: String,
    val application_name: String
    )

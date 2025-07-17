package com.houssein.sezaia.model.request

import android.app.Application

data class LoginRequest(
    val username: String,
    val password: String,
    val application_name: String
)
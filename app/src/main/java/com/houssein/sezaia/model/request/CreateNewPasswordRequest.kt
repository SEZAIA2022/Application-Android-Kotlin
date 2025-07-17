package com.houssein.sezaia.model.request

data class CreateNewPasswordRequest(
    val email: String,
    val new_password: String,
    val confirm_password: String,
    val application_name: String
)
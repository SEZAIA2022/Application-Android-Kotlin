package com.houssein.sezaia.model.request

data class CreateNewPasswordRequest(
    val token: String,
    val new_password: String,
    val confirm_password: String
)
package com.houssein.sezaia.model.request

data class ChangePasswordRequest (
    val email: String,
    val password: String,
    val new_password: String,
    val confirm_new_password: String
)

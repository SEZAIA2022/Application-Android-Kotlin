package com.houssein.sezaia.model.request

data class ChangeUsernameRequest(
    val username: String,
    val password: String,
    val new_username: String
)
package com.houssein.sezaia.model.request

data class DeleteAccountRequest (
    val email: String,
    val password: String
)
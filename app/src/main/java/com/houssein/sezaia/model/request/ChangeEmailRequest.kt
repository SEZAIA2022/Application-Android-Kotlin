package com.houssein.sezaia.model.request

data class ChangeEmailRequest(
    val email: String,
    val new_email: String,
    val password: String,
    val application_name : String

)
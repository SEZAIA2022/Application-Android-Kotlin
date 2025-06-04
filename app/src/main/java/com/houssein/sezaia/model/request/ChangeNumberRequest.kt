package com.houssein.sezaia.model.request

data class ChangeNumberRequest (
    val code: String,
    val phone: String,
    val new_code: String,
    val new_phone: String,
    val password: String
)

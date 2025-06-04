package com.houssein.sezaia.model.request

data class ChangeNumberRequest (
    val countryCode: String,
    val phoneNumber: String,
    val newCountryCode: String,
    val newPhoneNumber: String,
    val password: String
)

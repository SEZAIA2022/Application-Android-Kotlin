package com.houssein.sezaia.model.request


data class AddQrRequest(
    val username: String,
    val country: String,
    val city: String,
    val zone: String,
    val street: String,
    val exact_location: String,
    val qr_code: String
)

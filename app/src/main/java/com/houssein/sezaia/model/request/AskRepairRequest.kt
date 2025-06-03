package com.houssein.sezaia.model.request

data class AskRepairRequest(
    val username: String,
    val date: String,
    val comment: String,
    val qr_code: String
)

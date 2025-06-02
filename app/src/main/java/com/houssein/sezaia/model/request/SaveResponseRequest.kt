package com.houssein.sezaia.model.request

data class SaveResponseRequest(
    val question_id: String,
    val response: String,
    val username: String,
    val qr_code: String
)

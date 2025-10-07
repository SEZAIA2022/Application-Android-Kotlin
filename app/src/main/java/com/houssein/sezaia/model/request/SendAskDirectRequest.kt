package com.houssein.sezaia.model.request

data class SendAskDirectRequest(
    val username: String,
    val comment: String,
    val qr_id: String,
    val application_name: String,
    val technician_email: String
)
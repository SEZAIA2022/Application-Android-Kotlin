package com.houssein.sezaia.model.request

data class AskRepairWithResponsesRequest(
    val username: String,
    val date: String,
    val comment: String,
    val qr_code: String,
    val answers_json: String?,
    val application_name: String,
    val technician_email: String
)

package com.houssein.sezaia.model.request

data class AskRepairWithResponsesRequest(
    val username: String,
    val date: String,
    val comment: String,
    val qr_code: String,
    val responses: List<ResponseItem>
)
data class ResponseItem(
    val question_id: Int,
    val response: String
)
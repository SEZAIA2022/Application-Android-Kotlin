package com.houssein.sezaia.model.response

data class SendAskDirectResponse(
    val status: String,
    val message: String?,
    val ask_repair_id: Long?
)
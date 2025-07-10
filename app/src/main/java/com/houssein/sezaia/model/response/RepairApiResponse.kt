package com.houssein.sezaia.model.response

data class RepairApiResponse(
    val status: String,
    val data: List<Repair>?,
    val message: String?
)


package com.houssein.sezaia.model.response

data class RepairApiResponse(
    val status: String,
    val repairs: List<Repair>?,
    val message: String?
)


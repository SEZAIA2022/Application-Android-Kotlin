package com.houssein.sezaia.model.response

data class TakenSlotsResponse(
    val status: String,
    val total_techs: Int,
    val taken_slots: Map<String, Map<String, Int>>
)



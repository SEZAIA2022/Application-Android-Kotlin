package com.houssein.sezaia.model.response

data class TakenSlotsResponse(
    val status: String,
    val taken_slots: Map<String, List<String>>
)



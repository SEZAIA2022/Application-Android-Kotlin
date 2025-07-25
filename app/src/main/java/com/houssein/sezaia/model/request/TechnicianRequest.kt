package com.houssein.sezaia.model.request

data class TechnicianRequest(
    val email: String,
    val application_name: String,
    val date: String,
    val hour_slot: String
)
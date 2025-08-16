package com.houssein.sezaia.model.request

data class AssignAdminRequest(
    val email: String,
    val application_name: String,
    val date: String,       // "YYYY-MM-DD"
    val hour_slot: String,  // "HH:mm"
    val message: String? = null
)
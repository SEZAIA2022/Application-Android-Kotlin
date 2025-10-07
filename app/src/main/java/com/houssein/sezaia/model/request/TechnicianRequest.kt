package com.houssein.sezaia.model.request

data class TechnicianRequest(
    val email: String,
    val application_name: String? = null, // omis => backend déduit
    val date: String? = null,             // omis => backend choisit (>= now + 2h)
    val hour_slot: String? = null
)
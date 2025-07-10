package com.houssein.sezaia.model.response

data class Repair(
    val id: Int,
    val username: String,
    val date: String,
    val comment: String?,
    val qr_code: String,
    val hour_slot: String?,
    val status: String,
    val description_probleme: String?
)

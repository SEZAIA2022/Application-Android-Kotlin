package com.houssein.sezaia.model.response

data class Repair(
    val id: Int,
    val username: String,
    val date: String,
    val comment: String?,      // nullable si commentaire absent
    val qr_code: String,
    val hour_slot: String?,     // nullable si horaire absent
    val status: String
)

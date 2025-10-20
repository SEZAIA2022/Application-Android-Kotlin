package com.houssein.sezaia.model.response

import com.google.gson.annotations.SerializedName

data class Repair(
    val id: Int,
    val username: String,
    val date: String,
    val comment: String?,
    val qr_code: String,
    val hour_slot: String?,
    val status: String,
    val description_probleme: String?,
    val user_tech: String?,
    @SerializedName(
        value = "address",
        alternate = ["adresse", "address_line", "location", "addr"] // adapte si besoin
    )
    val address: String?
)

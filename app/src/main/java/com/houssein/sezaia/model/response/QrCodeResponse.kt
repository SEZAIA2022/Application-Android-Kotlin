package com.houssein.sezaia.model.response

data class QrCodeResponse(
    val status: String,
    val message: String,
    val is_active: Boolean,
    val qrcodes: List <String>,
    val qrids: List<String>,
    val status_repair: String,
    val id_ask_repair: String
)

data class QrIdResponse(
    val status: String,
    val message: String? = null,       // nullable (présent seulement en cas d’erreur)
    val qr_ids: List<String>? = null   // nullable (présent seulement en cas de succès)
)

data class QrIdResponses(
    val status: String,
    val qr_id: String?,
    val qr_code: String?,
    val application: String?,
    val message: String? = null
)
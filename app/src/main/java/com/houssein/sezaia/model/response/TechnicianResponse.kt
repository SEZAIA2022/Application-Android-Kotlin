package com.houssein.sezaia.model.response

data class TechnicianResponse(
    val status: String,
    val email: String?, // null si non trouvé
    val message: String? = null
)
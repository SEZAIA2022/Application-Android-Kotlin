package com.houssein.sezaia.model.response

data class AppData(
    val application: String?,
    val type: String?
)

data class AppNameTypeResponse(
    val status: String,
    val data: AppData?        // null si non trouvé
)


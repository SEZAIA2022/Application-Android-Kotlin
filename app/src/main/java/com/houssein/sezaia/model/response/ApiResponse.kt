package com.houssein.sezaia.model.response

data class ApiResponse(
    val status: String?,
    val message: String?,
    val errors: List<FieldError>? = null
)

data class FieldError(
    val field: Any,
    val message: String
)

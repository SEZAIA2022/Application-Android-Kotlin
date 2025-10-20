// app/src/main/java/com/houssein/sezaia/model/response/ProblemTypesResponse.kt
package com.houssein.sezaia.model.response

data class ProblemTypesResponse(
    val status: String,
    val application: String?,
    val types: List<String> = emptyList(),
    val message: String? = null
)

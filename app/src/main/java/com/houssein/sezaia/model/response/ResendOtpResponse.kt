package com.houssein.sezaia.model.response

import com.google.gson.annotations.SerializedName

data class ResendOtpResponse(
    @SerializedName("new_token") val newToken: String,
    val message: String,
    val status: String
)
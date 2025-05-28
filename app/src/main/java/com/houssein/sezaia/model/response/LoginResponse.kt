package com.houssein.sezaia.model.response

import android.provider.ContactsContract.CommonDataKinds.Email

data class LoginResponse(
    val message: String,
    val role: String,
    val user: String,
    val email: String
)
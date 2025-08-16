package com.houssein.sezaia.model.response

data class AssignAdminResponse(
    val status: String,
    val assigned_admin_email: String?,
    val notification_sent: Boolean?,
    val message: String? = null
)
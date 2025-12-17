package com.houssein.sezaia.model.response

data class HistoryResponse(
    val status: String,
    val data: HistoryData
)

data class HistoryData(
    val application: String,
    val qr_id: String,
    val serial_number: String,
    val username: String,
    val submissions: List<ReportSubmission>,
    val total: Int
)

data class ReportSubmission(
    val id: Int,
    val repport_id: Int,
    val application: String,
    val tech_user: String?,
    val qr_code: String?,
    val submitted_at: String?,
    val status: String,
    val answers: Map<String, Any>?
)

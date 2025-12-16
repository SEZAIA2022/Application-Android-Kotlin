package com.houssein.sezaia.model.response

data class HistoryResponse(
    val status: String,
    val data: HistoryData
)

data class HistoryData(
    val application: String,
    val submissions: List<ReportSubmission>,
    val total: Int
)

data class ReportSubmission(
    val id: Int,
    val repport_id: Int,
    val application: String,
    val title: String,
    val subtitle: String,
    val username: String?,
    val qr_code: String?,
    val submitted_at: String,  // ISO format: "2025-12-12T19:43:00"
    val status: String,         // "draft", "submitted", "completed"
    val answers: Map<String, Any>?
)

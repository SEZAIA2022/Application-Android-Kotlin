package com.houssein.sezaia.model.response

data class RepportMetaByIdsResponse(
    val status: String,
    val data: List<QuestionMetaRow>
)

data class QuestionMetaRow(
    val question_id: Int,
    val repport_id: Int,
    val application: String,
    val title: String,
    val subtitle: String
)


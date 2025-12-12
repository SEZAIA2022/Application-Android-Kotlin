package com.houssein.sezaia.model.response

// ---------- /repport/titles ----------
data class TitlesResponse(
    val status: String,
    val data: TitlesData
)

data class TitlesData(
    val application: String,
    val titles: List<String>
)

// ---------- /repport/subtitles ----------
data class SubtitlesResponse(
    val status: String,
    val data: SubtitlesData
)

data class SubtitlesData(
    val application: String,
    val title: String,
    val subtitles: List<String>
)

// ---------- /repport/questions ----------
data class QuestionsResponse(
    val status: String,
    val data: QuestionsData
)

data class QuestionsData(
    val application: String,
    val title: String,
    val subtitle: String,
    val repport_id: Int,
    val questions: List<QuestionDto>
)

// Question renvoyée par le backend
data class QuestionDto(
    val id: Int,
    val question_text: String,
    val question_type: String,   // "open", "qcm", "yes_no"
    val is_required: Boolean,
    val options: List<String>?   // liste des textes d’options (QCM)
)

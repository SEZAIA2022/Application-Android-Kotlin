package com.houssein.sezaia.model.response

data class HelpTask(
    val id: Int,
    val title_help: String,
    val help: String
)

data class HelpTasksResponse(
    val tasks: List<HelpTask>
)


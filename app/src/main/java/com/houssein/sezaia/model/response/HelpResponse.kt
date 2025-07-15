package com.houssein.sezaia.model.response

data class HelpItem(
    val id: Int,
    val title_help: String,
    val help: String,
    var isExpanded: Boolean = false // <-- par défaut false
)

data class HelpResponse(
    val tasks: List<HelpItem>
)

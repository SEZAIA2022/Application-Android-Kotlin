package com.houssein.sezaia.model

data class HelpItem(
    val title: String,
    val description: String,
    var isExpanded: Boolean = false
)

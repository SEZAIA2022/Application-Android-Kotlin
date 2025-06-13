package com.houssein.sezaia.model.data

import java.time.LocalDate

data class DayItem(
    val label: String,
    val timeSlots: List<String>,
    val localDate: LocalDate,         // Ajout du champ localDate pour garder la date
    var isSelected: Boolean = false
)

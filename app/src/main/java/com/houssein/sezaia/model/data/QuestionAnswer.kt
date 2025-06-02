package com.houssein.sezaia.model.data
import java.io.Serializable

// Serializable model
data class QuestionAnswer(
    val id: Int,
    val question: String,
    val answer: String
) : Serializable

package com.houssein.sezaia.model.response

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SimpleQuestionDto(
    val id: Int,
    val text: String,
    val application: String? = null,

    @SerializedName("machine_type_id")
    val machine_type_id: Int? = null,

    val question_type: String,

    // IMPORTANT: on le lit en JsonElement pour accepter 0/1 ou true/false
    @SerializedName("is_required")
    val is_required: JsonElement? = null,

    val options: List<String>? = emptyList()
) {
    fun isRequiredBoolean(): Boolean {
        val v = is_required ?: return false
        return when {
            v.isJsonPrimitive && v.asJsonPrimitive.isBoolean -> v.asBoolean
            v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asInt == 1
            v.isJsonPrimitive && v.asJsonPrimitive.isString -> v.asString == "1" || v.asString.equals("true", true)
            else -> false
        }
    }
}

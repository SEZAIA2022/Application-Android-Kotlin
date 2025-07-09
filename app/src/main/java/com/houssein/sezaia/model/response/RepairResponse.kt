data class RepairResponse(
    val repair: Repair,
    val responses: List<ResponseItem>
)

data class Repair(
    val id: Int,
    val username: String,
    val date: String?,
    val comment: String?,
    val qr_code: String?,
    val hour_slot: String?,
    val status: String?
)

data class ResponseItem(
    val response: String?,
    val question_id: Int,
    val question_text: String?
)

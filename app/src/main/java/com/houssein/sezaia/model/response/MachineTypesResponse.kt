package com.houssein.sezaia.model.response

data class MachineTypesResponse(
    val machine_types: List<MachineTypeDto> = emptyList()
)

data class MachineTypeDto(
    val id: Int,
    val type: String
)

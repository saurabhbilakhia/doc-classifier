package com.example.docai.admin.dto

import com.example.docai.common.enums.DataType
import com.example.docai.common.enums.RuleType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ClassificationRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    val description: String? = null,

    val priority: Int = 0,

    val threshold: Double = 0.5
)

data class ClassificationResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val priority: Int,
    val threshold: Double
)

data class PatternRequest(
    @field:NotBlank(message = "Pattern is required")
    val pattern: String,

    val flags: String? = null
)

data class PatternResponse(
    val id: Long,
    val pattern: String,
    val flags: String?
)

data class DataPointDefinitionRequest(
    @field:NotBlank(message = "Key is required")
    val key: String,

    val label: String? = null,

    @field:NotNull(message = "Type is required")
    val type: DataType,

    @field:NotNull(message = "Rule type is required")
    val ruleType: RuleType,

    @field:NotBlank(message = "Expression is required")
    val expression: String,

    val required: Boolean = false
)

data class DataPointDefinitionResponse(
    val id: Long,
    val key: String,
    val label: String?,
    val type: DataType,
    val ruleType: RuleType,
    val expression: String,
    val required: Boolean
)

package com.example.docai.extraction.engine

import com.example.docai.extraction.entities.DataPointDefinition

data class ExtractionContext(
    val rawText: String? = null,
    val json: Any? = null,
    val xml: org.w3c.dom.Document? = null
)

data class ExtractedValue(
    val raw: String,
    val confidence: Double = 0.9,
    val page: Int? = null,
    val spanStart: Int? = null,
    val spanEnd: Int? = null
)

interface ExtractionEngine {
    fun extract(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue?
}

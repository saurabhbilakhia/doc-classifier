package com.example.docai.document.dto

import com.example.docai.common.enums.DocumentStatus
import com.example.docai.document.entities.Document
import java.time.Instant

data class DocumentDto(
    val id: Long,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val status: DocumentStatus,
    val classificationName: String?,
    val summary: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(doc: Document): DocumentDto {
            return DocumentDto(
                id = doc.id!!,
                filename = doc.filename,
                mimeType = doc.mimeType,
                sizeBytes = doc.sizeBytes,
                status = doc.status,
                classificationName = doc.classification?.name,
                summary = doc.summary,
                createdAt = doc.createdAt,
                updatedAt = doc.updatedAt
            )
        }
    }
}

data class ExtractedDataPointDto(
    val id: Long,
    val key: String,
    val label: String?,
    val valueString: String?,
    val valueNumber: String?,
    val valueDate: String?,
    val confidence: Double?,
    val page: Int?,
    val spanStart: Int?,
    val spanEnd: Int?
)

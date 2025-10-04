package com.example.docai.extraction.repositories

import com.example.docai.extraction.entities.ExtractedDataPoint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExtractedDataPointRepository : JpaRepository<ExtractedDataPoint, Long> {
    fun findAllByDocumentId(documentId: Long, pageable: Pageable): Page<ExtractedDataPoint>
    fun findAllByDocumentId(documentId: Long): List<ExtractedDataPoint>
}

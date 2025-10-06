package com.example.docai.document.repositories

import com.example.docai.document.entities.DocumentText
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentTextRepository : JpaRepository<DocumentText, Long> {
    fun findByDocumentId(documentId: Long): DocumentText?
}

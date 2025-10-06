package com.example.docai.document.repositories

import com.example.docai.document.entities.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    fun findAllByOwnerId(ownerId: Long, pageable: Pageable): Page<Document>
}

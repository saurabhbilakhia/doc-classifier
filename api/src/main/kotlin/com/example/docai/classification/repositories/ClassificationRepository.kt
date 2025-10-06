package com.example.docai.classification.repositories

import com.example.docai.classification.entities.Classification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClassificationRepository : JpaRepository<Classification, Long> {
    fun findByName(name: String): Classification?
    fun existsByName(name: String): Boolean
}

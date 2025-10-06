package com.example.docai.classification.repositories

import com.example.docai.classification.entities.ClassificationPattern
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ClassificationPatternRepository : JpaRepository<ClassificationPattern, Long> {
    fun findAllByClassificationId(classificationId: Long): List<ClassificationPattern>

    @Query("SELECT p FROM ClassificationPattern p JOIN FETCH p.classification")
    fun findAllWithClassification(): List<ClassificationPattern>
}

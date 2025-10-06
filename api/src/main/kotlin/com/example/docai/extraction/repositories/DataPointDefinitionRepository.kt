package com.example.docai.extraction.repositories

import com.example.docai.extraction.entities.DataPointDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataPointDefinitionRepository : JpaRepository<DataPointDefinition, Long> {
    fun findAllByClassificationId(classificationId: Long): List<DataPointDefinition>
}

package com.example.docai.extraction.entities

import com.example.docai.classification.entities.Classification
import com.example.docai.document.entities.Document
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "extracted_data_points",
    indexes = [
        Index(name = "idx_extracted_doc", columnList = "document_id"),
        Index(name = "idx_extracted_key", columnList = "key")
    ]
)
class ExtractedDataPoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    var classification: Classification,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id", nullable = false)
    var definition: DataPointDefinition,

    @Column(nullable = false, length = 100)
    var key: String,

    @Column(name = "value_string", columnDefinition = "text")
    var valueString: String? = null,

    @Column(name = "value_number", precision = 19, scale = 4)
    var valueNumber: BigDecimal? = null,

    @Column(name = "value_date")
    var valueDate: LocalDate? = null,

    var confidence: Double? = null,

    var page: Int? = null,

    @Column(name = "span_start")
    var spanStart: Int? = null,

    @Column(name = "span_end")
    var spanEnd: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

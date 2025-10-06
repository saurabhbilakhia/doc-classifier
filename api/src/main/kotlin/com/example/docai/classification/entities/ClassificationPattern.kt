package com.example.docai.classification.entities

import jakarta.persistence.*

@Entity
@Table(name = "classification_patterns")
class ClassificationPattern(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    var classification: Classification,

    @Column(columnDefinition = "text", nullable = false)
    var pattern: String,

    @Column(length = 50)
    var flags: String? = null
)

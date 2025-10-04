package com.example.docai.classification.entities

import jakarta.persistence.*

@Entity
@Table(name = "classifications")
class Classification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(nullable = false)
    var priority: Int = 0,

    @Column(nullable = false)
    var threshold: Double = 0.5
)

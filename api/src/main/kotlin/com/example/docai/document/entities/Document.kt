package com.example.docai.document.entities

import com.example.docai.auth.entities.User
import com.example.docai.classification.entities.Classification
import com.example.docai.common.enums.DocumentStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "documents")
class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @Column(nullable = false, columnDefinition = "text")
    var filename: String,

    @Column(name = "mime_type", nullable = false, length = 200)
    var mimeType: String,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    var storageKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DocumentStatus = DocumentStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id")
    var classification: Classification? = null,

    @Column(columnDefinition = "text")
    var summary: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

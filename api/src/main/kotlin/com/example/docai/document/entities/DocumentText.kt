package com.example.docai.document.entities

import jakarta.persistence.*

@Entity
@Table(name = "document_texts")
class DocumentText(
    @Id
    @Column(name = "document_id")
    var documentId: Long,

    @Lob
    @Column(columnDefinition = "text", nullable = false)
    var text: String
)

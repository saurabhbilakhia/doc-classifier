package com.example.docai.document.controller

import com.example.docai.auth.repositories.UserRepository
import com.example.docai.common.enums.Role
import com.example.docai.document.dto.DocumentDto
import com.example.docai.document.dto.ExtractedDataPointDto
import com.example.docai.document.entities.Document
import com.example.docai.document.repositories.DocumentRepository
import com.example.docai.document.repositories.DocumentTextRepository
import com.example.docai.document.service.DocumentProcessingService
import com.example.docai.document.storage.DocumentStorage
import com.example.docai.extraction.repositories.ExtractedDataPointRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val storage: DocumentStorage,
    private val docRepo: DocumentRepository,
    private val docTextRepo: DocumentTextRepository,
    private val extractedRepo: ExtractedDataPointRepository,
    private val userRepo: UserRepository,
    private val processing: DocumentProcessingService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam("file") file: MultipartFile,
        principal: Principal
    ): ResponseEntity<DocumentDto> {
        require(!file.isEmpty) { "File is empty" }

        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")

        val key = storage.save(
            file.inputStream,
            file.originalFilename ?: "upload",
            file.contentType ?: "application/octet-stream"
        )

        val doc = Document(
            owner = user,
            filename = file.originalFilename ?: "upload",
            mimeType = file.contentType ?: "application/octet-stream",
            sizeBytes = file.size,
            storageKey = key
        )
        docRepo.save(doc)

        // Process synchronously for MVP
        try {
            processing.process(doc)
        } catch (e: Exception) {
            // Document status is already set to FAILED by the processing service
        }

        return ResponseEntity.ok(DocumentDto.from(doc))
    }

    @GetMapping("/{id}")
    fun getDocument(@PathVariable id: Long, principal: Principal): DocumentDto {
        val doc = docRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found") }

        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        // Check access: owner or admin
        if (doc.owner.id != user.id && user.role != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }

        return DocumentDto.from(doc)
    }

    @GetMapping
    fun listDocuments(principal: Principal, pageable: Pageable): Page<DocumentDto> {
        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        return if (user.role == Role.ADMIN) {
            docRepo.findAll(pageable).map { DocumentDto.from(it) }
        } else {
            docRepo.findAllByOwnerId(user.id!!, pageable).map { DocumentDto.from(it) }
        }
    }

    @GetMapping("/{id}/text")
    @PreAuthorize("hasRole('ADMIN')")
    fun getDocumentText(@PathVariable id: Long, principal: Principal): ResponseEntity<String> {
        val doc = docRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found") }

        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        // Admin or owner can access
        if (doc.owner.id != user.id && user.role != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        val docText = docTextRepo.findByDocumentId(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document text not found")

        return ResponseEntity.ok(docText.text)
    }

    @GetMapping("/{id}/extracted")
    fun getExtractedData(
        @PathVariable id: Long,
        principal: Principal,
        pageable: Pageable
    ): Page<ExtractedDataPointDto> {
        val doc = docRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found") }

        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        if (doc.owner.id != user.id && user.role != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        return extractedRepo.findAllByDocumentId(id, pageable).map { edp ->
            ExtractedDataPointDto(
                id = edp.id!!,
                key = edp.key,
                label = edp.definition.label,
                valueString = edp.valueString,
                valueNumber = edp.valueNumber?.toString(),
                valueDate = edp.valueDate?.toString(),
                confidence = edp.confidence,
                page = edp.page,
                spanStart = edp.spanStart,
                spanEnd = edp.spanEnd
            )
        }
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
        val doc = docRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found") }

        val user = userRepo.findByEmail(principal.name)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        if (doc.owner.id != user.id && user.role != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        storage.delete(doc.storageKey)
        docRepo.delete(doc)

        return ResponseEntity.noContent().build()
    }
}

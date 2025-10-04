package com.example.docai.document.parsing

import com.example.docai.document.entities.Document
import com.example.docai.document.storage.DocumentStorage
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.springframework.stereotype.Service

@Service
class TextExtractionService(
    private val storage: DocumentStorage
) {

    fun extract(document: Document): String {
        return storage.load(document.storageKey).use { input ->
            val handler = BodyContentHandler(-1) // No limit
            val metadata = Metadata()
            val parser = AutoDetectParser()
            val context = ParseContext()

            try {
                parser.parse(input, handler, metadata, context)
                handler.toString().trim()
            } catch (e: Exception) {
                throw RuntimeException("Failed to extract text from document: ${document.filename}", e)
            }
        }
    }
}

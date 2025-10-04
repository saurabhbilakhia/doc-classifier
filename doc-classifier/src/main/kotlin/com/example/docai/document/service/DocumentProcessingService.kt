package com.example.docai.document.service

import com.example.docai.classification.engine.RuleBasedClassificationEngine
import com.example.docai.classification.repositories.ClassificationRepository
import com.example.docai.common.enums.DataType
import com.example.docai.common.enums.DocumentStatus
import com.example.docai.document.entities.Document
import com.example.docai.document.entities.DocumentText
import com.example.docai.document.parsing.TextExtractionService
import com.example.docai.document.repositories.DocumentRepository
import com.example.docai.document.repositories.DocumentTextRepository
import com.example.docai.document.summarization.SummarizationService
import com.example.docai.extraction.engine.ExtractionContext
import com.example.docai.extraction.engine.RuleBasedExtractionEngine
import com.example.docai.extraction.entities.ExtractedDataPoint
import com.example.docai.extraction.repositories.DataPointDefinitionRepository
import com.example.docai.extraction.repositories.ExtractedDataPointRepository
import com.jayway.jsonpath.Configuration
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.xml.sax.InputSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Service
class DocumentProcessingService(
    private val textExtractor: TextExtractionService,
    private val classificationEngine: RuleBasedClassificationEngine,
    private val extractionEngine: RuleBasedExtractionEngine,
    private val summarizer: SummarizationService,
    private val classificationRepo: ClassificationRepository,
    private val dpDefRepo: DataPointDefinitionRepository,
    private val docRepo: DocumentRepository,
    private val docTextRepo: DocumentTextRepository,
    private val extractedRepo: ExtractedDataPointRepository
) {

    @Transactional
    fun process(document: Document) {
        try {
            document.status = DocumentStatus.PROCESSING
            docRepo.save(document)

            // 1. Parse/Extract text
            val text = textExtractor.extract(document)
            docTextRepo.save(DocumentText(documentId = document.id!!, text = text))

            // 2. Classify
            val candidates = classificationEngine.loadCandidates()
            val best = classificationEngine.classify(text, candidates)
            val classification = if (best != null && best.score >= best.classification.threshold) {
                best.classification
            } else {
                classificationRepo.findByName("undefined")
                    ?: throw IllegalStateException("Undefined classification not found")
            }
            document.classification = classification

            // 3. Extract data points
            val definitions = dpDefRepo.findAllByClassificationId(classification.id!!)
            val ctx = ExtractionContext(
                rawText = text,
                json = tryParseJson(text),
                xml = tryParseXml(text)
            )

            definitions.forEach { def ->
                val value = extractionEngine.extract(def, ctx)
                if (value != null) {
                    val edp = ExtractedDataPoint(
                        document = document,
                        classification = classification,
                        definition = def,
                        key = def.key
                    )

                    // Type coercion
                    when (def.type) {
                        DataType.NUMBER -> edp.valueNumber = value.raw.toBigDecimalOrNull()
                        DataType.DATE -> edp.valueDate = tryParseDate(value.raw)
                        DataType.CURRENCY -> edp.valueNumber = value.raw
                            .replace(Regex("[^0-9.]"), "")
                            .toBigDecimalOrNull()
                        DataType.BOOLEAN -> edp.valueString = value.raw.lowercase() in listOf("true", "yes", "1")
                            .let { if (it) "true" else "false" }
                        else -> edp.valueString = value.raw
                    }

                    edp.confidence = value.confidence
                    edp.page = value.page
                    edp.spanStart = value.spanStart
                    edp.spanEnd = value.spanEnd
                    extractedRepo.save(edp)
                }
            }

            // 4. Summarize
            document.summary = summarizer.summarize(text)

            // 5. Complete
            document.status = DocumentStatus.COMPLETED
            document.updatedAt = Instant.now()
            docRepo.save(document)

        } catch (e: Exception) {
            document.status = DocumentStatus.FAILED
            document.updatedAt = Instant.now()
            docRepo.save(document)
            throw RuntimeException("Document processing failed for ${document.filename}", e)
        }
    }

    private fun tryParseJson(text: String): Any? {
        return try {
            Configuration.defaultConfiguration().jsonProvider().parse(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParseXml(text: String): org.w3c.dom.Document? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            dbf.newDocumentBuilder().parse(InputSource(text.reader()))
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParseDate(raw: String): LocalDate? {
        val formats = listOf("yyyy-MM-dd", "MM/dd/yyyy", "dd MMM yyyy", "dd-MM-yyyy")
        for (format in formats) {
            try {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern(format))
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
}

package com.example.docai.admin.controller

import com.example.docai.admin.dto.*
import com.example.docai.classification.entities.Classification
import com.example.docai.classification.entities.ClassificationPattern
import com.example.docai.classification.repositories.ClassificationPatternRepository
import com.example.docai.classification.repositories.ClassificationRepository
import com.example.docai.extraction.entities.DataPointDefinition
import com.example.docai.extraction.repositories.DataPointDefinitionRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/classifications")
@PreAuthorize("hasRole('ADMIN')")
class ClassificationAdminController(
    private val classificationRepo: ClassificationRepository,
    private val patternRepo: ClassificationPatternRepository,
    private val dpDefRepo: DataPointDefinitionRepository
) {

    @GetMapping
    fun listClassifications(): List<ClassificationResponse> {
        return classificationRepo.findAll().map { cls ->
            ClassificationResponse(
                id = cls.id!!,
                name = cls.name,
                description = cls.description,
                priority = cls.priority,
                threshold = cls.threshold
            )
        }
    }

    @PostMapping
    fun createClassification(@Valid @RequestBody request: ClassificationRequest): ClassificationResponse {
        if (classificationRepo.existsByName(request.name)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Classification with this name already exists")
        }

        val classification = Classification(
            name = request.name,
            description = request.description,
            priority = request.priority,
            threshold = request.threshold
        )
        classificationRepo.save(classification)

        return ClassificationResponse(
            id = classification.id!!,
            name = classification.name,
            description = classification.description,
            priority = classification.priority,
            threshold = classification.threshold
        )
    }

    @PutMapping("/{id}")
    fun updateClassification(
        @PathVariable id: Long,
        @Valid @RequestBody request: ClassificationRequest
    ): ClassificationResponse {
        val classification = classificationRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found") }

        if (classification.name == "undefined") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify the 'undefined' classification")
        }

        classification.name = request.name
        classification.description = request.description
        classification.priority = request.priority
        classification.threshold = request.threshold
        classificationRepo.save(classification)

        return ClassificationResponse(
            id = classification.id!!,
            name = classification.name,
            description = classification.description,
            priority = classification.priority,
            threshold = classification.threshold
        )
    }

    @DeleteMapping("/{id}")
    fun deleteClassification(@PathVariable id: Long) {
        val classification = classificationRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found") }

        if (classification.name == "undefined") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the 'undefined' classification")
        }

        classificationRepo.delete(classification)
    }

    @GetMapping("/{id}/patterns")
    fun listPatterns(@PathVariable id: Long): List<PatternResponse> {
        if (!classificationRepo.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found")
        }

        return patternRepo.findAllByClassificationId(id).map { p ->
            PatternResponse(
                id = p.id!!,
                pattern = p.pattern,
                flags = p.flags
            )
        }
    }

    @PostMapping("/{id}/patterns")
    fun addPatterns(
        @PathVariable id: Long,
        @Valid @RequestBody requests: List<PatternRequest>
    ): List<PatternResponse> {
        val classification = classificationRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found") }

        val patterns = requests.map { req ->
            ClassificationPattern(
                classification = classification,
                pattern = req.pattern,
                flags = req.flags
            )
        }
        patternRepo.saveAll(patterns)

        return patterns.map { p ->
            PatternResponse(
                id = p.id!!,
                pattern = p.pattern,
                flags = p.flags
            )
        }
    }

    @DeleteMapping("/{classificationId}/patterns/{patternId}")
    fun deletePattern(@PathVariable classificationId: Long, @PathVariable patternId: Long) {
        val pattern = patternRepo.findById(patternId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found") }

        if (pattern.classification.id != classificationId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Pattern does not belong to this classification")
        }

        patternRepo.delete(pattern)
    }

    @GetMapping("/{id}/datapoints")
    fun listDataPoints(@PathVariable id: Long): List<DataPointDefinitionResponse> {
        if (!classificationRepo.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found")
        }

        return dpDefRepo.findAllByClassificationId(id).map { dpd ->
            DataPointDefinitionResponse(
                id = dpd.id!!,
                key = dpd.key,
                label = dpd.label,
                type = dpd.type,
                ruleType = dpd.ruleType,
                expression = dpd.expression,
                required = dpd.required
            )
        }
    }

    @PostMapping("/{id}/datapoints")
    fun addDataPoints(
        @PathVariable id: Long,
        @Valid @RequestBody requests: List<DataPointDefinitionRequest>
    ): List<DataPointDefinitionResponse> {
        val classification = classificationRepo.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Classification not found") }

        val dataPoints = requests.map { req ->
            DataPointDefinition(
                classification = classification,
                key = req.key,
                label = req.label,
                type = req.type,
                ruleType = req.ruleType,
                expression = req.expression,
                required = req.required
            )
        }
        dpDefRepo.saveAll(dataPoints)

        return dataPoints.map { dpd ->
            DataPointDefinitionResponse(
                id = dpd.id!!,
                key = dpd.key,
                label = dpd.label,
                type = dpd.type,
                ruleType = dpd.ruleType,
                expression = dpd.expression,
                required = dpd.required
            )
        }
    }

    @PutMapping("/datapoints/{dpId}")
    fun updateDataPoint(
        @PathVariable dpId: Long,
        @Valid @RequestBody request: DataPointDefinitionRequest
    ): DataPointDefinitionResponse {
        val dpDef = dpDefRepo.findById(dpId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Data point definition not found") }

        dpDef.key = request.key
        dpDef.label = request.label
        dpDef.type = request.type
        dpDef.ruleType = request.ruleType
        dpDef.expression = request.expression
        dpDef.required = request.required
        dpDefRepo.save(dpDef)

        return DataPointDefinitionResponse(
            id = dpDef.id!!,
            key = dpDef.key,
            label = dpDef.label,
            type = dpDef.type,
            ruleType = dpDef.ruleType,
            expression = dpDef.expression,
            required = dpDef.required
        )
    }

    @DeleteMapping("/datapoints/{dpId}")
    fun deleteDataPoint(@PathVariable dpId: Long) {
        if (!dpDefRepo.existsById(dpId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Data point definition not found")
        }
        dpDefRepo.deleteById(dpId)
    }
}

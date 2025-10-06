package com.example.docai.classification.engine

import com.example.docai.classification.entities.Classification

data class ClassificationResult(
    val classification: Classification,
    val score: Double
)

data class ClassificationWithPatterns(
    val classification: Classification,
    val patterns: List<Regex>
)

interface ClassificationEngine {
    fun classify(text: String, candidates: List<ClassificationWithPatterns>): ClassificationResult?
}

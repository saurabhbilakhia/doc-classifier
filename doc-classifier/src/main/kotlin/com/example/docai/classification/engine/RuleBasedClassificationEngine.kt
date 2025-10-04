package com.example.docai.classification.engine

import com.example.docai.classification.repositories.ClassificationPatternRepository
import com.example.docai.classification.repositories.ClassificationRepository
import org.springframework.stereotype.Service

@Service
class RuleBasedClassificationEngine(
    private val classificationRepo: ClassificationRepository,
    private val patternRepo: ClassificationPatternRepository
) : ClassificationEngine {

    override fun classify(text: String, candidates: List<ClassificationWithPatterns>): ClassificationResult? {
        var best: ClassificationResult? = null

        for (candidate in candidates) {
            if (candidate.patterns.isEmpty()) continue

            val hits = candidate.patterns.count { it.containsMatchIn(text) }
            if (hits > 0) {
                // Score: normalized matches + priority bonus
                val normalizedMatches = hits.toDouble() / candidate.patterns.size
                val priorityBonus = candidate.classification.priority / 100.0
                val score = normalizedMatches + priorityBonus

                if (best == null || score > best.score) {
                    best = ClassificationResult(candidate.classification, score)
                }
            }
        }

        return best
    }

    fun loadCandidates(): List<ClassificationWithPatterns> {
        val patterns = patternRepo.findAllWithClassification()
        val grouped = patterns.groupBy { it.classification }

        return grouped.map { (classification, patternList) ->
            ClassificationWithPatterns(
                classification = classification,
                patterns = patternList.map { p ->
                    try {
                        Regex(p.pattern, RegexOption.MULTILINE)
                    } catch (e: Exception) {
                        // Skip invalid patterns
                        null
                    }
                }.filterNotNull()
            )
        }.filter { it.patterns.isNotEmpty() }
    }
}

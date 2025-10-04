package com.example.docai.document.summarization

import org.springframework.stereotype.Service
import kotlin.math.ln

@Service
class SummarizationService {

    fun summarize(text: String, maxSentences: Int = 5): String {
        val sentences = text
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .take(500)

        if (sentences.isEmpty()) return ""
        if (sentences.size <= maxSentences) return sentences.joinToString(" ")

        // Tokenize sentences
        val tf = mutableMapOf<String, Int>()
        val docs = sentences.map { sentence ->
            val tokens = sentence.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 }
            tokens.forEach { token -> tf[token] = (tf[token] ?: 0) + 1 }
            tokens
        }

        // Calculate document frequency
        val df = mutableMapOf<String, Int>()
        docs.forEach { tokens ->
            tokens.toSet().forEach { token ->
                df[token] = (df[token] ?: 0) + 1
            }
        }

        val n = sentences.size.toDouble()

        // Score sentences using TF-IDF
        val scored = sentences.mapIndexed { index, _ ->
            val tokens = docs[index]
            val score = tokens.sumOf { token ->
                val termFreq = tokens.count { it == token }.toDouble()
                val docFreq = (df[token] ?: 1).toDouble()
                (termFreq / tokens.size) * ln(n / docFreq)
            } + (if (index < 3) 0.5 else 0.0) // Lead bias

            index to score
        }

        // Select top sentences and return in original order
        val topIndices = scored
            .sortedByDescending { it.second }
            .take(maxSentences)
            .map { it.first }
            .sorted()

        return topIndices.joinToString(" ") { sentences[it] }
    }
}

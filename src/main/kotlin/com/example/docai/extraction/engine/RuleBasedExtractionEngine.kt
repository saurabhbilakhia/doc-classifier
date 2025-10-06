package com.example.docai.extraction.engine

import com.example.docai.common.enums.RuleType
import com.example.docai.extraction.entities.DataPointDefinition
import com.jayway.jsonpath.JsonPath
import org.springframework.stereotype.Service
import javax.xml.xpath.XPathFactory

@Service
class RuleBasedExtractionEngine : ExtractionEngine {

    override fun extract(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue? {
        return when (def.ruleType) {
            RuleType.REGEX -> extractByRegex(def, ctx)
            RuleType.JSON_PATH -> extractByJsonPath(def, ctx)
            RuleType.XPATH -> extractByXPath(def, ctx)
        }
    }

    private fun extractByRegex(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue? {
        val text = ctx.rawText ?: return null
        val regex = try {
            Regex(def.expression, RegexOption.MULTILINE)
        } catch (e: Exception) {
            return null
        }

        val match = regex.find(text) ?: return null
        val value = match.groups[1]?.value ?: match.value

        return ExtractedValue(
            raw = value,
            spanStart = match.range.first,
            spanEnd = match.range.last
        )
    }

    private fun extractByJsonPath(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue? {
        val json = ctx.json ?: return null
        return try {
            val value = JsonPath.read<Any?>(json, def.expression) ?: return null
            ExtractedValue(raw = value.toString())
        } catch (e: Exception) {
            null
        }
    }

    private fun extractByXPath(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue? {
        val doc = ctx.xml ?: return null
        return try {
            val xPath = XPathFactory.newInstance().newXPath()
            val value = xPath.evaluate(def.expression, doc)
            if (value.isNullOrBlank()) null else ExtractedValue(raw = value)
        } catch (e: Exception) {
            null
        }
    }
}

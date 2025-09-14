package com.chessrl.integration.metrics

import com.chessrl.integration.writeTextFile
import com.chessrl.integration.appendTextFile

object MetricsExporter {
    // CSV export (header inferred from all field keys). Returns CSV string for convenience.
    fun toCsvString(records: List<MetricRecord>): String {
        if (records.isEmpty()) return ""
        val keySet = linkedSetOf<String>()
        records.forEach { keySet.addAll(it.fields.keys) }
        val headers = listOf("timestamp", "category", "tags") + keySet.toList()
        val sb = StringBuilder()
        sb.append(headers.joinToString(",")).append('\n')
        for (r in records) {
            val row = mutableListOf<String>()
            row.add(r.timestamp.toString())
            row.add(r.category)
            row.add(encodeTags(r.tags))
            for (k in keySet) {
                row.add(valueToCsv(r.fields[k]))
            }
            sb.append(row.joinToString(",")).append('\n')
        }
        return sb.toString()
    }

    fun writeCsv(path: String, records: List<MetricRecord>) {
        writeTextFile(path, toCsvString(records))
    }

    // NDJSON export (one JSON object per line). Returns string for convenience.
    fun toJsonLinesString(records: List<MetricRecord>): String {
        val sb = StringBuilder()
        for (r in records) {
            sb.append(recordToJson(r)).append('\n')
        }
        return sb.toString()
    }

    fun writeJsonLines(path: String, records: List<MetricRecord>, append: Boolean = false) {
        val content = toJsonLinesString(records)
        if (append) appendTextFile(path, content) else writeTextFile(path, content)
    }

    private fun encodeTags(tags: Map<String, String>): String {
        if (tags.isEmpty()) return ""
        return tags.entries.joinToString(";") { e ->
            e.key.replace(",", "_") + "=" + e.value.replace(",", "_")
        }
    }

    private fun valueToCsv(v: Any?): String = when (v) {
        null -> ""
        is Number, is Boolean -> v.toString()
        else -> '"' + v.toString().replace("\"", "\"\"") + '"'
    }

    private fun recordToJson(r: MetricRecord): String {
        val fieldsJson = mapToJson(r.fields)
        val tagsJson = mapToJson(r.tags)
        return "{" +
            "\"timestamp\":${r.timestamp}," +
            "\"category\":${stringToJson(r.category)}," +
            "\"fields\":$fieldsJson," +
            "\"tags\":$tagsJson" +
        "}"
    }

    private fun mapToJson(map: Map<String, Any?>): String {
        val parts = mutableListOf<String>()
        for ((k, v) in map) {
            parts.add("\"" + escapeJson(k) + "\":" + anyToJson(v))
        }
        return "{" + parts.joinToString(",") + "}"
    }

    private fun anyToJson(v: Any?): String = when (v) {
        null -> "null"
        is Number, is Boolean -> v.toString()
        is String -> stringToJson(v)
        is Map<*, *> -> @Suppress("UNCHECKED_CAST") mapToJson(v as Map<String, Any?>)
        is List<*> -> listToJson(v)
        is Array<*> -> listToJson(v.toList())
        else -> stringToJson(v.toString())
    }

    private fun listToJson(list: List<*>): String {
        val parts = list.map { anyToJson(it) }
        return "[" + parts.joinToString(",") + "]"
    }

    private fun stringToJson(s: String): String = "\"" + escapeJson(s) + "\""
    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}


package com.ora.rescue

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class GeneratedQuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/** Generates a grounded quiz from the exact material linked to a completed session. */
class GeminiQuizGenerator(private val contentResolver: ContentResolver) {
    suspend fun generate(material: CourseMaterial, chapterTitle: String): List<GeneratedQuizQuestion> = withContext(Dispatchers.IO) {
        require(BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            "Gemini is not configured. Add GEMINI_API_KEY to ~/.gradle/gradle.properties and rebuild."
        }
        require(material.mimeType == "application/pdf" || material.mimeType.startsWith("text/")) {
            "Gemini quiz generation currently supports PDF and text files."
        }
        val bytes = contentResolver.openInputStream(Uri.parse(material.sourceUri))?.use { it.readBytes(MAX_BYTES + 1) }
            ?: error("Ora Rescue could not reopen this course material.")
        require(bytes.size <= MAX_BYTES) { "Course material must be 10 MB or smaller for this app build." }

        val prompt = """
            Create exactly five multiple-choice questions grounded only in the attached course material.
            Focus on the selected chapter or topic: $chapterTitle.
            Each item needs a clear prompt, exactly four distinct options, a correctIndex from 0 to 3,
            and a short teaching explanation. Do not invent facts absent from the material.
        """.trimIndent()
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(JSONObject().put("inlineData", JSONObject()
                .put("mimeType", material.mimeType)
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))))
        val itemSchema = JSONObject().put("type", "OBJECT")
            .put("required", JSONArray(listOf("prompt", "options", "correctIndex", "explanation")))
            .put("properties", JSONObject()
                .put("prompt", JSONObject().put("type", "STRING"))
                .put("options", JSONObject().put("type", "ARRAY").put("minItems", 4).put("maxItems", 4)
                    .put("items", JSONObject().put("type", "STRING")))
                .put("correctIndex", JSONObject().put("type", "INTEGER").put("minimum", 0).put("maximum", 3))
                .put("explanation", JSONObject().put("type", "STRING")))
        val request = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put("generationConfig", JSONObject()
                .put("temperature", 0.2)
                .put("responseMimeType", "application/json")
                .put("responseSchema", JSONObject().put("type", "ARRAY").put("minItems", 5).put("maxItems", 5).put("items", itemSchema)))

        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            connection.outputStream.use { it.write(request.toString().toByteArray()) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            require(connection.responseCode in 200..299) { apiError(response) }
            val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            parse(text)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(json: String): List<GeneratedQuizQuestion> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val choices = item.optJSONArray("options") ?: return@mapNotNull null
            val options = (0 until choices.length()).map { choices.optString(it).trim() }
            val answer = item.optInt("correctIndex", -1)
            val prompt = item.optString("prompt").trim()
            if (prompt.isBlank() || options.size != 4 || options.any { it.isBlank() } || answer !in 0..3) null
            else GeneratedQuizQuestion(
                prompt, options, answer,
                item.optString("explanation").trim().ifBlank { "Review this section in the course material." }
            )
        }.also { require(it.size >= 3) { "Gemini returned too few valid questions. Please try again." } }
    }

    private fun apiError(body: String): String = runCatching {
        JSONObject(body).getJSONObject("error").getString("message")
    }.getOrDefault("Gemini could not generate this quiz. Check the API key and connection.")

    private companion object { const val MAX_BYTES = 10 * 1024 * 1024 }
}

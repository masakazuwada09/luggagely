package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "MuseSparkMetaClient"
private const val META_MODEL_API_URL = "https://api.meta.ai/v1/chat/completions"

object MuseSparkMetaClient {

    const val MUSE_SPARK_MODEL_ID = "muse-spark-1.3"

    fun isMetaApiKeyConfigured(): Boolean {
        val raw = runCatching { BuildConfig.META_API_KEY }.getOrNull().orEmpty().trim()
        return raw.isNotBlank() && raw != "YOUR_META_API_KEY" && raw != "null"
    }

    private fun candidateBearerTokens(): List<String> {
        val raw = runCatching { BuildConfig.META_API_KEY }.getOrNull().orEmpty().trim()
        if (raw.isBlank() || raw == "YOUR_META_API_KEY" || raw == "null") return emptyList()
        return if (raw.startsWith("APILLM_")) {
            listOf(raw.removePrefix("API"), raw).distinct()
        } else {
            listOf(raw)
        }
    }

    suspend fun analyzeFrameWithMuseSpark(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val tokens = candidateBearerTokens()
        if (tokens.isEmpty()) {
            throw IllegalStateException("META_API_KEY is not configured in BuildConfig.")
        }

        val base64Jpeg = encodeBitmapToBase64Jpeg(bitmap)
        val developerPrompt = """
            You are a Meta SAM single-object segmentation and weight estimation engine ($MUSE_SPARK_MODEL_ID).
            Analyze the camera image, segment ONLY THE SINGLE MOST PROMINENT ITEM in the foreground, and return ONLY valid JSON with this exact structure (no markdown fences):
            {
              "model": "$MUSE_SPARK_MODEL_ID",
              "summary": "1-sentence description of the single segmented item",
              "items": [
                {
                  "name": "Specific identified item name",
                  "weightKg": 0.85,
                  "category": "COMPUTE | POWER | APPAREL | LIQUIDS | ACCESSORY",
                  "confidence": 0.98,
                  "left": 0.22,
                  "top": 0.20,
                  "width": 0.56,
                  "height": 0.54
                }
              ]
            }
        """.trimIndent()

        var lastError: Exception? = null
        for (token in tokens) {
            try {
                val rawContent = executeChatCompletion(
                    token = token,
                    developerPrompt = developerPrompt,
                    base64Jpeg = base64Jpeg,
                    includeResponseFormat = true
                )
                return@withContext extractCleanJsonObject(rawContent)
            } catch (firstErr: Exception) {
                Log.w(TAG, "Muse Spark ($MUSE_SPARK_MODEL_ID) structured attempt warning: ${firstErr.message}")
                try {
                    val fallbackContent = executeChatCompletion(
                        token = token,
                        developerPrompt = developerPrompt,
                        base64Jpeg = base64Jpeg,
                        includeResponseFormat = false
                    )
                    return@withContext extractCleanJsonObject(fallbackContent)
                } catch (secondErr: Exception) {
                    lastError = secondErr
                }
            }
        }

        throw lastError ?: IllegalStateException("Muse Spark ($MUSE_SPARK_MODEL_ID) request failed")
    }

    private fun executeChatCompletion(
        token: String,
        developerPrompt: String,
        base64Jpeg: String,
        includeResponseFormat: Boolean
    ): String {
        val payload = JSONObject().apply {
            put("model", MUSE_SPARK_MODEL_ID)
            put("reasoning_effort", "minimal")
            if (includeResponseFormat) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val messages = JSONArray()
            messages.put(
                JSONObject().apply {
                    put("role", "developer")
                    put("content", developerPrompt)
                }
            )
            messages.put(
                JSONObject().apply {
                    put("role", "user")
                    val contentParts = JSONArray()
                    contentParts.put(
                        JSONObject().apply {
                            put("type", "text")
                            put(
                                "text",
                                "Using Meta SAM segmentation, detect, segment, and label ONLY ONE primary item in the center/foreground of this camera frame. Return JSON with summary and a 1-element items array (name, weightKg, category, confidence, left, top, width, height)."
                            )
                        }
                    )
                    contentParts.put(
                        JSONObject().apply {
                            put("type", "image_url")
                            put(
                                "image_url",
                                JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg")
                            )
                        }
                    )
                    put("content", contentParts)
                }
            )
            put("messages", messages)
        }

        val connection = (URL(META_MODEL_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 40_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.w(TAG, "Muse Spark ($MUSE_SPARK_MODEL_ID) HTTP $status: $err")
                throw IllegalStateException("Muse Spark ($MUSE_SPARK_MODEL_ID) HTTP $status")
            }

            val responseJson = JSONObject(body)
            val choices = responseJson.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content").orEmpty().trim()
            if (content.isBlank()) {
                throw IllegalStateException("Empty content from $MUSE_SPARK_MODEL_ID")
            }
            return content
        } finally {
            connection.disconnect()
        }
    }

    suspend fun analyzeAirlineBookingWithMuse(
        carrierName: String,
        iataCode: String,
        bookingCode: String,
        passengerName: String,
        passengerEmail: String,
        packedWeightKg: Double,
        packedItemNames: List<String>
    ): String = withContext(Dispatchers.IO) {
        val tokens = candidateBearerTokens()
        if (tokens.isEmpty()) {
            throw IllegalStateException("META_API_KEY is not configured in BuildConfig.")
        }

        val itemsDescription = if (packedItemNames.isEmpty()) {
            "Empty manifest (0.0 kg currently packed)"
        } else {
            packedItemNames.joinToString(", ")
        }

        val developerPrompt = """
            You are the Muse Spark ($MUSE_SPARK_MODEL_ID) Airline Booking & Baggage Inclusion Intelligence Engine.
            Resolve and analyze the traveler's verified airline booking record for carrier "$carrierName ($iataCode)" matched to Passenger Name "$passengerName", Registered Email "$passengerEmail", and Booking Reference / PNR "$bookingCode".
            Current packed luggage weight: $packedWeightKg kg ($itemsDescription).
            Return ONLY valid JSON (no markdown fences) with this exact structure:
            {
              "model": "$MUSE_SPARK_MODEL_ID",
              "flightNumber": "${iataCode} 567",
              "route": "MNL (Manila) -> CEB / NRT / KIX",
              "fareBundle": "Official airline fare tier name",
              "eTicketNumber": "13-digit IATA e-ticket number",
              "departureSchedule": "Departure date & time in local airport time",
              "terminalGate": "Terminal and Boarding Gate details",
              "seatNumber": "Assigned seat number and zone",
              "cabinInclusionKg": 7.0,
              "cabinInclusionDetails": "Exact cabin carry-on & personal item inclusion rule for $carrierName",
              "checkedInclusionKg": 20.0,
              "checkedInclusionDetails": "Exact checked baggage inclusion rule for $carrierName booking $bookingCode",
              "headline": "Short status headline for $passengerName ($passengerEmail) comparing $packedWeightKg kg against $carrierName inclusions",
              "summary": "2-sentence Muse Spark ($MUSE_SPARK_MODEL_ID) analysis of baggage inclusion and packing optimization for passenger $passengerName ($passengerEmail) on $carrierName booking $bookingCode",
              "policyTips": [
                "Specific $carrierName baggage rule tip 1",
                "Specific $carrierName baggage rule tip 2",
                "Specific $carrierName baggage rule tip 3"
              ]
            }
        """.trimIndent()

        val userPrompt = "Retrieve and analyze real airline booking details for passenger Name '$passengerName' and Email '$passengerEmail' (PNR $bookingCode) on $carrierName ($iataCode) vs current $packedWeightKg kg manifest ($itemsDescription) using $MUSE_SPARK_MODEL_ID."

        var lastError: Exception? = null
        for (token in tokens) {
            try {
                val rawContent = executeTextCompletion(
                    token = token,
                    developerPrompt = developerPrompt,
                    userPrompt = userPrompt,
                    includeResponseFormat = true
                )
                return@withContext extractCleanJsonObject(rawContent)
            } catch (firstErr: Exception) {
                try {
                    val fallbackContent = executeTextCompletion(
                        token = token,
                        developerPrompt = developerPrompt,
                        userPrompt = userPrompt,
                        includeResponseFormat = false
                    )
                    return@withContext extractCleanJsonObject(fallbackContent)
                } catch (secondErr: Exception) {
                    lastError = secondErr
                }
            }
        }

        throw lastError ?: IllegalStateException("Muse Spark ($MUSE_SPARK_MODEL_ID) booking analysis failed")
    }

    private fun executeTextCompletion(
        token: String,
        developerPrompt: String,
        userPrompt: String,
        includeResponseFormat: Boolean
    ): String {
        val payload = JSONObject().apply {
            put("model", MUSE_SPARK_MODEL_ID)
            put("reasoning_effort", "minimal")
            if (includeResponseFormat) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val messages = JSONArray()
            messages.put(
                JSONObject().apply {
                    put("role", "developer")
                    put("content", developerPrompt)
                }
            )
            messages.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                }
            )
            put("messages", messages)
        }

        val connection = (URL(META_MODEL_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.w(TAG, "Muse Spark ($MUSE_SPARK_MODEL_ID) Booking HTTP $status: $err")
                throw IllegalStateException("Muse Spark ($MUSE_SPARK_MODEL_ID) HTTP $status")
            }

            val responseJson = JSONObject(body)
            val choices = responseJson.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content").orEmpty().trim()
            if (content.isBlank()) {
                throw IllegalStateException("Empty content from $MUSE_SPARK_MODEL_ID")
            }
            return content
        } finally {
            connection.disconnect()
        }
    }

    fun extractCleanJsonObject(raw: String): String {
        val trimmed = raw.trim()
        val startIdx = trimmed.indexOf('{')
        val endIdx = trimmed.lastIndexOf('}')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return trimmed.substring(startIdx, endIdx + 1)
        }
        return trimmed
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

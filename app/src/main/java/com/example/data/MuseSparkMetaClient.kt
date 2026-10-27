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
private const val MUSE_SPARK_MODEL_ID = "muse-spark-1.3"

object MuseSparkMetaClient {

    fun isMetaApiKeyConfigured(): Boolean {
        val raw = runCatching { BuildConfig.META_API_KEY }.getOrNull().orEmpty().trim()
        return raw.isNotBlank() && raw != "YOUR_META_API_KEY" && raw != "null"
    }

    private fun resolveBearerToken(): String {
        val raw = runCatching { BuildConfig.META_API_KEY }.getOrNull().orEmpty().trim()
        return if (raw.startsWith("APILLM_")) {
            raw.removePrefix("API")
        } else {
            raw
        }
    }

    suspend fun analyzeFrameWithMuseSpark(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val token = resolveBearerToken()
        if (token.isBlank() || token == "YOUR_META_API_KEY") {
            throw IllegalStateException("META_API_KEY is not configured in BuildConfig.")
        }

        val base64Jpeg = encodeBitmapToBase64Jpeg(bitmap)
        val systemPrompt = """
            You are the Meta SAM 3.1 + Muse Spark 1.3 real-time vision segmentation and gravimetric mass inference engine.
            Analyze the camera image, segment all visible objects/gear/luggage/items, and return ONLY valid JSON with this exact structure:
            {
              "summary": "1-sentence summary of the segmented items identified by Muse Spark & Meta SAM",
              "items": [
                {
                  "name": "Specific identified item name",
                  "weightKg": 1.25,
                  "category": "COMPUTE | POWER | APPAREL | LIQUIDS | ACCESSORY",
                  "confidence": 0.97,
                  "left": 0.12,
                  "top": 0.18,
                  "width": 0.34,
                  "height": 0.32
                }
              ]
            }
        """.trimIndent()

        val payload = JSONObject().apply {
            put("model", MUSE_SPARK_MODEL_ID)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
            val messages = JSONArray()
            messages.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
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
                                "Segment and identify all objects in this camera frame using Muse Spark and Meta SAM segmentation. Provide bounding boxes (left, top, width, height in 0.04..0.90) and estimated mass in kg."
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
            readTimeout = 35_000
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
                Log.w(TAG, "Muse Spark Meta API returned HTTP $status: $err")
                throw IllegalStateException("Muse Spark Meta API HTTP $status")
            }

            val responseJson = JSONObject(body)
            val choices = responseJson.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val content = message?.optString("content").orEmpty().trim()
            if (content.isBlank()) {
                throw IllegalStateException("Empty content from Muse Spark Meta API")
            }
            content
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

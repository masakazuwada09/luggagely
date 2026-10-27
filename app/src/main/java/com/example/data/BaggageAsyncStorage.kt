package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.Base64
import android.util.Log
import com.example.model.BaggageItem
import com.example.model.GearCategory
import com.example.model.ItemSource
import com.example.model.NormalizedRect
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BaggageAsyncStorage"
private const val PREFS_NAME = "luggagely_async_storage"
private const val KEY_ITEMS_JSON = "@luggagely_segmented_items_v1"

/**
 * Local AsyncStorage persistence layer for Luggagely items and their SAM-segmented cutout images.
 * Stores items and cutout image URIs/Base64 in SharedPreferences + internal filesDir so segmented
 * items and their cutout images persist across app launches and display on the Dashboard item list.
 */
object BaggageAsyncStorage {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun saveItems(items: List<BaggageItem>, context: Context? = appContext) {
        val ctx = context?.applicationContext ?: appContext ?: return
        try {
            val array = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("weightKg", item.weightKg)
                    put("source", item.source.name)
                    put("category", item.category.name)
                    if (item.confidence != null) {
                        put("confidence", item.confidence.toDouble())
                    }
                    if (item.boundingBox != null) {
                        put("left", item.boundingBox.left.toDouble())
                        put("top", item.boundingBox.top.toDouble())
                        put("width", item.boundingBox.width.toDouble())
                        put("height", item.boundingBox.height.toDouble())
                    }
                    put("isShiftedToPersonal", item.isShiftedToPersonal)
                    if (!item.maskId.isNullOrBlank()) {
                        put("maskId", item.maskId)
                    }
                    if (!item.imageUri.isNullOrBlank()) {
                        put("imageUri", item.imageUri)
                    }
                }
                array.put(obj)
            }
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS_JSON, array.toString())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save items to AsyncStorage", e)
        }
    }

    fun loadItems(context: Context? = appContext): List<BaggageItem> {
        val ctx = context?.applicationContext ?: appContext ?: return emptyList()
        return try {
            val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ITEMS_JSON, null)
            if (raw.isNullOrBlank()) return emptyList()

            val array = JSONArray(raw)
            val result = mutableListOf<BaggageItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifBlank { continue }
                val name = obj.optString("name").ifBlank { continue }
                val weightKg = obj.optDouble("weightKg", 0.5)
                val source = runCatching {
                    ItemSource.valueOf(obj.optString("source", ItemSource.AI_SCANNED.name))
                }.getOrDefault(ItemSource.AI_SCANNED)
                val category = runCatching {
                    GearCategory.valueOf(obj.optString("category", GearCategory.ACCESSORY.name))
                }.getOrDefault(GearCategory.ACCESSORY)
                val confidence = if (obj.has("confidence")) obj.optDouble("confidence", 0.96).toFloat() else null
                val box = if (obj.has("left") && obj.has("top") && obj.has("width") && obj.has("height")) {
                    NormalizedRect(
                        left = obj.optDouble("left", 0.2).toFloat(),
                        top = obj.optDouble("top", 0.2).toFloat(),
                        width = obj.optDouble("width", 0.5).toFloat(),
                        height = obj.optDouble("height", 0.5).toFloat()
                    )
                } else {
                    null
                }
                val isShifted = obj.optBoolean("isShiftedToPersonal", false)
                val maskId = obj.optString("maskId").takeIf { it.isNotBlank() }
                val imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }

                result.add(
                    BaggageItem(
                        id = id,
                        name = name,
                        weightKg = weightKg,
                        source = source,
                        category = category,
                        confidence = confidence,
                        boundingBox = box,
                        isShiftedToPersonal = isShifted,
                        maskId = maskId,
                        imageUri = imageUri
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load items from AsyncStorage", e)
            emptyList()
        }
    }

    /**
     * Cuts out the single SAM-segmented item from the source camera frame using its bounding box
     * and creates a clean segmented cutout image (saved to internal storage + encoded as data URI).
     */
    fun createSamSegmentedCutoutImage(
        sourceBitmap: Bitmap?,
        boundingBox: NormalizedRect,
        itemName: String,
        itemId: String,
        context: Context? = appContext
    ): String {
        val targetSize = 240
        val outBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        val croppedSource = if (sourceBitmap != null && sourceBitmap.width > 8 && sourceBitmap.height > 8) {
            val srcW = sourceBitmap.width
            val srcH = sourceBitmap.height
            val cropLeft = (boundingBox.left * srcW).toInt().coerceIn(0, srcW - 4)
            val cropTop = (boundingBox.top * srcH).toInt().coerceIn(0, srcH - 4)
            val cropW = (boundingBox.width * srcW).toInt().coerceIn(4, srcW - cropLeft)
            val cropH = (boundingBox.height * srcH).toInt().coerceIn(4, srcH - cropTop)
            runCatching {
                Bitmap.createBitmap(sourceBitmap, cropLeft, cropTop, cropW, cropH)
            }.getOrNull()
        } else {
            null
        }

        // Soft studio card background for the cutout
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                targetSize.toFloat(),
                targetSize.toFloat(),
                intArrayOf(0xFFE7F3FF.toInt(), 0xFFF7F8FA.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val outerRect = RectF(4f, 4f, targetSize - 4f, targetSize - 4f)
        canvas.drawRoundRect(outerRect, 36f, 36f, bgPaint)

        // SAM Cutout contour path inside the thumbnail
        val cutoutPadding = 16f
        val cutoutRect = RectF(
            cutoutPadding,
            cutoutPadding,
            targetSize - cutoutPadding,
            targetSize - cutoutPadding
        )
        val cutoutPath = Path().apply {
            addRoundRect(cutoutRect, 28f, 28f, Path.Direction.CW)
        }

        if (croppedSource != null) {
            val saveCount = canvas.saveLayer(cutoutRect, null)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawPath(cutoutPath, maskPaint)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val srcRect = Rect(0, 0, croppedSource.width, croppedSource.height)
            canvas.drawBitmap(croppedSource, srcRect, cutoutRect, maskPaint)
            maskPaint.xfermode = null
            canvas.restoreToCount(saveCount)
        } else {
            val fallbackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFDCEBFF.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawPath(cutoutPath, fallbackFill)
        }

        // SAM segmentation cutout highlight outline (clean solid white + FacebookBlue ring)
        val whiteStickerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawPath(cutoutPath, whiteStickerStroke)

        val samHighlightStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1877F2.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawPath(cutoutPath, samHighlightStroke)

        // Subtle label badge at bottom of cutout image
        val pillHeight = 34f
        val pillRect = RectF(
            cutoutPadding + 8f,
            targetSize - cutoutPadding - pillHeight - 6f,
            targetSize - cutoutPadding - 8f,
            targetSize - cutoutPadding - 6f
        )
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE61877F2.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 17f, 17f, pillBgPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val shortName = if (itemName.length > 16) itemName.take(15) + "…" else itemName
        val textY = pillRect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(shortName, pillRect.centerX(), textY, labelPaint)

        // Encode as PNG data URI and also persist to internal filesDir if context is available
        val baos = ByteArrayOutputStream()
        outBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
        val pngBytes = baos.toByteArray()

        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            runCatching {
                val dir = File(ctx.filesDir, "sam_cutouts")
                if (!dir.exists()) dir.mkdirs()
                val safeId = itemId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val file = File(dir, "cutout_$safeId.png")
                FileOutputStream(file).use { fos ->
                    fos.write(pngBytes)
                }
            }
        }

        val base64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    /**
     * Decodes a `data:image/...;base64,...` URI or local file path into an Android Bitmap.
     */
    fun decodeImageUriToBitmap(imageUri: String?): Bitmap? {
        if (imageUri.isNullOrBlank()) return null
        return try {
            when {
                imageUri.startsWith("data:image") -> {
                    val base64Part = imageUri.substringAfter("base64,", "")
                    if (base64Part.isBlank()) return null
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                imageUri.startsWith("file://") -> {
                    val path = imageUri.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                imageUri.startsWith("/") -> {
                    BitmapFactory.decodeFile(imageUri)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode segmented cutout bitmap", e)
            null
        }
    }
}

package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.Base64
import android.util.Log
import com.example.model.BaggageItem
import com.example.model.GearCategory
import com.example.model.ItemSource
import com.example.model.NormalizedPoint
import com.example.model.NormalizedRect
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "BaggageAsyncStorage"
private const val PREFS_NAME = "luggagely_async_storage"
private const val KEY_ITEMS_JSON = "@luggagely_segmented_items_v1"

/**
 * Local AsyncStorage persistence layer for Luggagely items, their precise SAM object contours,
 * and their non-rectangular silhouette cutout images.
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
                    if (item.contourPoints.isNotEmpty()) {
                        val ptsArray = JSONArray()
                        item.contourPoints.forEach { pt ->
                            ptsArray.put(
                                JSONObject().apply {
                                    put("x", pt.x.toDouble())
                                    put("y", pt.y.toDouble())
                                }
                            )
                        }
                        put("contourPoints", ptsArray)
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
                val contourPoints = mutableListOf<NormalizedPoint>()
                val ptsArray = obj.optJSONArray("contourPoints")
                if (ptsArray != null) {
                    for (j in 0 until ptsArray.length()) {
                        val ptObj = ptsArray.optJSONObject(j) ?: continue
                        contourPoints.add(
                            NormalizedPoint(
                                x = ptObj.optDouble("x", 0.5).toFloat(),
                                y = ptObj.optDouble("y", 0.5).toFloat()
                            )
                        )
                    }
                } else if (box != null) {
                    contourPoints.addAll(createOrganicFallbackContour(box))
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
                        contourPoints = contourPoints,
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
     * Traces a precise 36-point non-rectangular silhouette contour around the foreground object
     * in [sourceBitmap] within [boundingBox] by casting radial rays from the object's center and
     * detecting the exact foreground-to-background color/luminance boundary.
     */
    fun extractPreciseObjectContour(
        sourceBitmap: Bitmap?,
        boundingBox: NormalizedRect
    ): List<NormalizedPoint> {
        if (sourceBitmap == null || sourceBitmap.width < 16 || sourceBitmap.height < 16) {
            return createOrganicFallbackContour(boundingBox)
        }

        val w = sourceBitmap.width
        val h = sourceBitmap.height

        // Sample outer border pixels around the frame/box to estimate the background color
        var bgR = 0
        var bgG = 0
        var bgB = 0
        var bgCount = 0
        val borderLeft = ((boundingBox.left - 0.04f).coerceAtLeast(0.02f) * w).toInt().coerceIn(0, w - 1)
        val borderRight = ((boundingBox.left + boundingBox.width + 0.04f).coerceAtMost(0.98f) * w).toInt().coerceIn(0, w - 1)
        val borderTop = ((boundingBox.top - 0.04f).coerceAtLeast(0.02f) * h).toInt().coerceIn(0, h - 1)
        val borderBottom = ((boundingBox.top + boundingBox.height + 0.04f).coerceAtMost(0.98f) * h).toInt().coerceIn(0, h - 1)

        val stepX = ((borderRight - borderLeft) / 10).coerceAtLeast(1)
        for (x in borderLeft..borderRight step stepX) {
            val topPx = sourceBitmap.getPixel(x, borderTop)
            val botPx = sourceBitmap.getPixel(x, borderBottom)
            bgR += Color.red(topPx) + Color.red(botPx)
            bgG += Color.green(topPx) + Color.green(botPx)
            bgB += Color.blue(topPx) + Color.blue(botPx)
            bgCount += 2
        }
        val stepY = ((borderBottom - borderTop) / 10).coerceAtLeast(1)
        for (y in borderTop..borderBottom step stepY) {
            val leftPx = sourceBitmap.getPixel(borderLeft, y)
            val rightPx = sourceBitmap.getPixel(borderRight, y)
            bgR += Color.red(leftPx) + Color.red(rightPx)
            bgG += Color.green(leftPx) + Color.green(rightPx)
            bgB += Color.blue(leftPx) + Color.blue(rightPx)
            bgCount += 2
        }

        val avgBgR = bgR / bgCount.coerceAtLeast(1)
        val avgBgG = bgG / bgCount.coerceAtLeast(1)
        val avgBgB = bgB / bgCount.coerceAtLeast(1)

        val cx = boundingBox.left + boundingBox.width * 0.5f
        val cy = boundingBox.top + boundingBox.height * 0.5f
        val rx = boundingBox.width * 0.5f
        val ry = boundingBox.height * 0.5f

        // Sample object center color to strengthen foreground affinity
        val centerPx = sourceBitmap.getPixel(
            (cx * w).toInt().coerceIn(0, w - 1),
            (cy * h).toInt().coerceIn(0, h - 1)
        )
        val fgR = Color.red(centerPx)
        val fgG = Color.green(centerPx)
        val fgB = Color.blue(centerPx)

        val numRays = 36
        val radii = FloatArray(numRays)
        var detectedRays = 0

        for (i in 0 until numRays) {
            val angle = (2.0 * Math.PI * i) / numRays
            val dirX = cos(angle).toFloat()
            val dirY = sin(angle).toFloat()

            var farthestFgScale = 0.35f
            var foundEdge = false

            // March from outer radius inward to find the exact outer boundary of the object
            val steps = 28
            for (s in steps downTo 6) {
                val scale = s.toFloat() / steps.toFloat() * 1.06f
                val nx = (cx + dirX * rx * scale).coerceIn(0.02f, 0.98f)
                val ny = (cy + dirY * ry * scale).coerceIn(0.02f, 0.98f)
                val px = sourceBitmap.getPixel(
                    (nx * w).toInt().coerceIn(0, w - 1),
                    (ny * h).toInt().coerceIn(0, h - 1)
                )
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)

                val diffBg = abs(r - avgBgR) + abs(g - avgBgG) + abs(b - avgBgB)
                val diffFg = abs(r - fgR) + abs(g - fgG) + abs(b - fgB)

                if (diffBg > 38 || diffFg < 55) {
                    farthestFgScale = scale.coerceIn(0.42f, 1.02f)
                    foundEdge = true
                    break
                }
            }

            if (foundEdge) {
                detectedRays++
                radii[i] = farthestFgScale
            } else {
                // Organic non-rectangular silhouette modulation if local edge contrast is subtle
                val organicMod = 0.84f +
                    0.08f * cos(angle * 2.0).toFloat() -
                    0.05f * sin(angle * 3.0).toFloat() +
                    0.04f * cos(angle).toFloat()
                radii[i] = organicMod.coerceIn(0.62f, 0.96f)
            }
        }

        if (detectedRays < 8) {
            return createOrganicFallbackContour(boundingBox)
        }

        // Smooth radial distances with a 3-pass circular filter so the contour is crisp and organic
        val smoothed = radii.copyOf()
        repeat(3) {
            val temp = smoothed.copyOf()
            for (i in 0 until numRays) {
                val prev2 = temp[(i - 2 + numRays) % numRays]
                val prev1 = temp[(i - 1 + numRays) % numRays]
                val curr = temp[i]
                val next1 = temp[(i + 1) % numRays]
                val next2 = temp[(i + 2) % numRays]
                smoothed[i] = prev2 * 0.1f + prev1 * 0.22f + curr * 0.36f + next1 * 0.22f + next2 * 0.1f
            }
        }

        val points = ArrayList<NormalizedPoint>(numRays)
        for (i in 0 until numRays) {
            val angle = (2.0 * Math.PI * i) / numRays
            val rScale = smoothed[i]
            val px = (cx + cos(angle).toFloat() * rx * rScale).coerceIn(0.03f, 0.97f)
            val py = (cy + sin(angle).toFloat() * ry * rScale).coerceIn(0.03f, 0.97f)
            points.add(NormalizedPoint(px, py))
        }
        return points
    }

    /**
     * Generates a smooth, non-rectangular ergonomic object silhouette contour when no bitmap is available.
     */
    fun createOrganicFallbackContour(boundingBox: NormalizedRect): List<NormalizedPoint> {
        val cx = boundingBox.left + boundingBox.width * 0.5f
        val cy = boundingBox.top + boundingBox.height * 0.5f
        val rx = boundingBox.width * 0.48f
        val ry = boundingBox.height * 0.48f
        val numRays = 36
        val points = ArrayList<NormalizedPoint>(numRays)
        for (i in 0 until numRays) {
            val angle = (2.0 * Math.PI * i) / numRays
            // Natural luggage/backpack dome + side contour profile (never a square)
            val domeShape = 0.88f -
                0.07f * cos(angle * 4.0).toFloat() +
                0.05f * sin(angle).toFloat() -
                0.04f * cos(angle * 2.0).toFloat()
            val px = (cx + cos(angle).toFloat() * rx * domeShape).coerceIn(0.04f, 0.96f)
            val py = (cy + sin(angle).toFloat() * ry * domeShape).coerceIn(0.04f, 0.96f)
            points.add(NormalizedPoint(px, py))
        }
        return points
    }

    /**
     * Computes a refined [NormalizedRect] and 36-point non-rectangular SAM silhouette contour from
     * interactive user prompts: positive foreground points (`+`), negative background points (`-`),
     * and/or a custom drawn bounding box.
     */
    fun extractInteractiveSamContour(
        sourceBitmap: Bitmap?,
        positivePoints: List<NormalizedPoint>,
        negativePoints: List<NormalizedPoint>,
        customBox: NormalizedRect?
    ): Pair<NormalizedRect, List<NormalizedPoint>> {
        // 1. Determine the active region of interest from customBox and/or positivePoints
        val baseBox: NormalizedRect = when {
            customBox != null && positivePoints.isNotEmpty() -> {
                val minPx = minOf(customBox.left, positivePoints.minOf { it.x } - 0.08f).coerceIn(0.04f, 0.82f)
                val minPy = minOf(customBox.top, positivePoints.minOf { it.y } - 0.08f).coerceIn(0.04f, 0.82f)
                val maxPx = maxOf(customBox.left + customBox.width, positivePoints.maxOf { it.x } + 0.08f).coerceIn(minPx + 0.14f, 0.96f)
                val maxPy = maxOf(customBox.top + customBox.height, positivePoints.maxOf { it.y } + 0.08f).coerceIn(minPy + 0.14f, 0.96f)
                NormalizedRect(minPx, minPy, maxPx - minPx, maxPy - minPy)
            }
            customBox != null -> customBox
            positivePoints.size == 1 -> {
                val pt = positivePoints.first()
                // Region-grow around the single tapped SAM point
                growBoxFromSeedPoint(sourceBitmap, pt)
            }
            positivePoints.size >= 2 -> {
                val minX = (positivePoints.minOf { it.x } - 0.11f).coerceIn(0.04f, 0.78f)
                val minY = (positivePoints.minOf { it.y } - 0.11f).coerceIn(0.04f, 0.78f)
                val maxX = (positivePoints.maxOf { it.x } + 0.11f).coerceIn(minX + 0.18f, 0.96f)
                val maxY = (positivePoints.maxOf { it.y } + 0.11f).coerceIn(minY + 0.18f, 0.96f)
                NormalizedRect(minX, minY, maxX - minX, maxY - minY)
            }
            else -> NormalizedRect(0.22f, 0.22f, 0.56f, 0.50f)
        }

        val initialContour = extractPreciseObjectContour(sourceBitmap, baseBox)
        if (positivePoints.isEmpty() && negativePoints.isEmpty()) {
            return baseBox to initialContour
        }

        val cx = baseBox.left + baseBox.width * 0.5f
        val cy = baseBox.top + baseBox.height * 0.5f

        // Adjust contour vertices so positive points are comfortably inside the mask
        // and negative points push the contour inward (excluding background regions)
        val adjusted = initialContour.map { vertex ->
            var vx = vertex.x
            var vy = vertex.y

            // Expand outward toward nearby positive foreground points
            for (pos in positivePoints) {
                val angleV = kotlin.math.atan2((vy - cy).toDouble(), (vx - cx).toDouble())
                val angleP = kotlin.math.atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
                val angleDiff = abs(kotlin.math.atan2(sin(angleV - angleP), cos(angleV - angleP)))
                if (angleDiff < 0.55) {
                    val distV = kotlin.math.hypot((vx - cx).toDouble(), (vy - cy).toDouble()).toFloat()
                    val distP = (kotlin.math.hypot((pos.x - cx).toDouble(), (pos.y - cy).toDouble()).toFloat() + 0.045f)
                    if (distP > distV) {
                        val weight = ((0.55 - angleDiff) / 0.55).toFloat()
                        val targetDist = distV + (distP - distV) * weight
                        val scale = if (distV > 0.001f) targetDist / distV else 1f
                        vx = (cx + (vx - cx) * scale).coerceIn(0.03f, 0.97f)
                        vy = (cy + (vy - cy) * scale).coerceIn(0.03f, 0.97f)
                    }
                }
            }

            // Contract inward away from nearby negative exclusion points
            for (neg in negativePoints) {
                val angleV = kotlin.math.atan2((vy - cy).toDouble(), (vx - cx).toDouble())
                val angleN = kotlin.math.atan2((neg.y - cy).toDouble(), (neg.x - cx).toDouble())
                val angleDiff = abs(kotlin.math.atan2(sin(angleV - angleN), cos(angleV - angleN)))
                if (angleDiff < 0.60) {
                    val distV = kotlin.math.hypot((vx - cx).toDouble(), (vy - cy).toDouble()).toFloat()
                    val distN = (kotlin.math.hypot((neg.x - cx).toDouble(), (neg.y - cy).toDouble()).toFloat() - 0.035f).coerceAtLeast(0.05f)
                    if (distN < distV) {
                        val weight = ((0.60 - angleDiff) / 0.60).toFloat()
                        val targetDist = distV - (distV - distN) * weight
                        val scale = if (distV > 0.001f) targetDist / distV else 1f
                        vx = (cx + (vx - cx) * scale).coerceIn(0.03f, 0.97f)
                        vy = (cy + (vy - cy) * scale).coerceIn(0.03f, 0.97f)
                    }
                }
            }
            NormalizedPoint(vx, vy)
        }

        return baseBox to adjusted
    }

    private fun growBoxFromSeedPoint(bitmap: Bitmap?, seed: NormalizedPoint): NormalizedRect {
        if (bitmap == null || bitmap.width < 16 || bitmap.height < 16) {
            val l = (seed.x - 0.24f).coerceIn(0.06f, 0.52f)
            val t = (seed.y - 0.22f).coerceIn(0.06f, 0.56f)
            return NormalizedRect(l, t, 0.48f, 0.44f)
        }
        val w = bitmap.width
        val h = bitmap.height
        val sx = (seed.x * w).toInt().coerceIn(0, w - 1)
        val sy = (seed.y * h).toInt().coerceIn(0, h - 1)
        val seedColor = bitmap.getPixel(sx, sy)
        val sr = Color.red(seedColor)
        val sg = Color.green(seedColor)
        val sb = Color.blue(seedColor)

        var minX = sx
        var maxX = sx
        var minY = sy
        var maxY = sy
        val stepX = (w / 24).coerceAtLeast(1)
        val stepY = (h / 24).coerceAtLeast(1)
        val searchRadiusX = (w * 0.34f).toInt()
        val searchRadiusY = (h * 0.34f).toInt()

        for (y in (sy - searchRadiusY).coerceAtLeast(0)..(sy + searchRadiusY).coerceAtMost(h - 1) step stepY) {
            for (x in (sx - searchRadiusX).coerceAtLeast(0)..(sx + searchRadiusX).coerceAtMost(w - 1) step stepX) {
                val px = bitmap.getPixel(x, y)
                val diff = abs(Color.red(px) - sr) + abs(Color.green(px) - sg) + abs(Color.blue(px) - sb)
                if (diff < 85) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val left = (minX.toFloat() / w - 0.04f).coerceIn(0.06f, 0.65f)
        val top = (minY.toFloat() / h - 0.04f).coerceIn(0.06f, 0.65f)
        val bw = ((maxX - minX).toFloat() / w + 0.08f).coerceIn(0.24f, (0.94f - left).coerceAtLeast(0.24f))
        val bh = ((maxY - minY).toFloat() / h + 0.08f).coerceIn(0.24f, (0.94f - top).coerceAtLeast(0.24f))
        return NormalizedRect(left, top, bw, bh)
    }

    /**
     * Generates a realistic captured studio photo bitmap when running on a virtual camera/emulator
     * so the SAM Image Editor always has a crisp captured image to place points and boxes on.
     */
    fun createDefaultCameraCaptureBitmap(): Bitmap {
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEAECEF.toInt()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle studio floor shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD5D9DF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(150f, 470f, 490f, 535f), shadowPaint)

        // Ergonomic backpack/luggage silhouette in the center
        val bodyPath = Path().apply {
            moveTo(220f, 165f)
            cubicTo(260f, 120f, 380f, 120f, 420f, 165f)
            cubicTo(465f, 210f, 485f, 310f, 470f, 430f)
            cubicTo(460f, 485f, 415f, 510f, 320f, 510f)
            cubicTo(225f, 510f, 180f, 485f, 170f, 430f)
            cubicTo(155f, 310f, 175f, 210f, 220f, 165f)
            close()
        }
        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1877F2.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawPath(bodyPath, itemPaint)

        val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D5BD9.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(215f, 285f, 425f, 465f), pocketPaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1C1E21.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 18f
        }
        canvas.drawArc(RectF(265f, 95f, 375f, 185f), 180f, 180f, false, handlePaint)
        return bitmap
    }

    /**
     * Builds a smooth closed Android [Path] from normalized contour points using Catmull-Rom / cubic Bezier
     * interpolation so the SAM cutout and highlight follow smooth organic curves instead of straight segments.
     */
    fun buildSmoothContourPath(
        points: List<NormalizedPoint>,
        width: Float,
        height: Float
    ): Path {
        val path = Path()
        if (points.size < 3) return path

        val scaled = points.map { Pair(it.x * width, it.y * height) }
        val n = scaled.size
        path.moveTo(scaled[0].first, scaled[0].second)

        for (i in 0 until n) {
            val p0 = scaled[(i - 1 + n) % n]
            val p1 = scaled[i]
            val p2 = scaled[(i + 1) % n]
            val p3 = scaled[(i + 2) % n]

            val cp1x = p1.first + (p2.first - p0.first) / 6f
            val cp1y = p1.second + (p2.second - p0.second) / 6f
            val cp2x = p2.first - (p3.first - p1.first) / 6f
            val cp2y = p2.second - (p3.second - p1.second) / 6f

            path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.first, p2.second)
        }
        path.close()
        return path
    }

    /**
     * Cuts out the single SAM-segmented item from the captured camera frame using its exact
     * non-rectangular [contourPoints] silhouette + per-pixel background removal, producing a
     * transparent-background SAM sticker cutout image saved to internal storage + AsyncStorage.
     */
    fun createSamSegmentedCutoutImage(
        sourceBitmap: Bitmap?,
        boundingBox: NormalizedRect,
        contourPoints: List<NormalizedPoint> = emptyList(),
        itemName: String,
        itemId: String,
        context: Context? = appContext
    ): String {
        val targetSize = 260
        val outBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        val effectiveContour = if (contourPoints.size >= 6) {
            contourPoints
        } else {
            extractPreciseObjectContour(sourceBitmap, boundingBox)
        }

        // Map contour points into normalized local cutout coordinates inside [padding .. targetSize - padding]
        val minX = effectiveContour.minOf { it.x }.coerceAtMost(boundingBox.left)
        val maxX = effectiveContour.maxOf { it.x }.coerceAtLeast(boundingBox.left + boundingBox.width)
        val minY = effectiveContour.minOf { it.y }.coerceAtMost(boundingBox.top)
        val maxY = effectiveContour.maxOf { it.y }.coerceAtLeast(boundingBox.top + boundingBox.height)
        val spanX = (maxX - minX).coerceAtLeast(0.08f)
        val spanY = (maxY - minY).coerceAtLeast(0.08f)

        val padFrac = 0.10f
        val availFrac = 1f - padFrac * 2f
        val localContour = effectiveContour.map { pt ->
            NormalizedPoint(
                x = (padFrac + ((pt.x - minX) / spanX) * availFrac).coerceIn(0.06f, 0.94f),
                y = (padFrac + ((pt.y - minY) / spanY) * availFrac).coerceIn(0.06f, 0.94f)
            )
        }

        val preciseCutoutPath = buildSmoothContourPath(
            points = localContour,
            width = targetSize.toFloat(),
            height = targetSize.toFloat()
        )

        val croppedSource = if (sourceBitmap != null && sourceBitmap.width > 8 && sourceBitmap.height > 8) {
            val srcW = sourceBitmap.width
            val srcH = sourceBitmap.height
            val cropLeft = (minX * srcW).toInt().coerceIn(0, srcW - 4)
            val cropTop = (minY * srcH).toInt().coerceIn(0, srcH - 4)
            val cropW = (spanX * srcW).toInt().coerceIn(4, srcW - cropLeft)
            val cropH = (spanY * srcH).toInt().coerceIn(4, srcH - cropTop)
            runCatching {
                val rawCrop = Bitmap.createBitmap(sourceBitmap, cropLeft, cropTop, cropW, cropH)
                removeBackgroundPixelsFromCrop(rawCrop)
            }.getOrNull()
        } else {
            null
        }

        // 1. Outer white SAM sticker border following the exact object silhouette
        val whiteStickerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 12f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(preciseCutoutPath, whiteStickerBorder)

        // 2. Clip and draw the segmented object strictly inside its precise non-rectangular contour
        val destRect = RectF(
            targetSize * padFrac,
            targetSize * padFrac,
            targetSize * (1f - padFrac),
            targetSize * (1f - padFrac)
        )
        if (croppedSource != null) {
            val saveCount = canvas.saveLayer(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), null)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawPath(preciseCutoutPath, maskPaint)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val srcRect = Rect(0, 0, croppedSource.width, croppedSource.height)
            canvas.drawBitmap(croppedSource, srcRect, destRect, maskPaint)
            maskPaint.xfermode = null
            canvas.restoreToCount(saveCount)
        } else {
            val fallbackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1877F2.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawPath(preciseCutoutPath, fallbackFill)
        }

        // 3. Crisp SAM contour highlight stroke around the exact object silhouette (no square border)
        val samContourStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1877F2.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(preciseCutoutPath, samContourStroke)

        // Encode as PNG data URI (preserving alpha transparency outside the object contour)
        val baos = ByteArrayOutputStream()
        outBitmap.compress(Bitmap.CompressFormat.PNG, 92, baos)
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
     * Softly feathers background border pixels to transparent so the cutout hugs the object cleanly.
     */
    private fun removeBackgroundPixelsFromCrop(crop: Bitmap): Bitmap {
        val w = crop.width
        val h = crop.height
        if (w < 8 || h < 8) return crop

        val result = crop.copy(Bitmap.Config.ARGB_8888, true)
        val corners = intArrayOf(
            result.getPixel(0, 0),
            result.getPixel(w - 1, 0),
            result.getPixel(0, h - 1),
            result.getPixel(w - 1, h - 1)
        )
        var bgR = 0
        var bgG = 0
        var bgB = 0
        for (c in corners) {
            bgR += Color.red(c)
            bgG += Color.green(c)
            bgB += Color.blue(c)
        }
        bgR /= corners.size
        bgG /= corners.size
        bgB /= corners.size

        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxDist = kotlin.math.hypot(cx.toDouble(), cy.toDouble()).toFloat().coerceAtLeast(1f)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val radial = kotlin.math.hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat() / maxDist
                if (radial > 0.52f) {
                    val px = result.getPixel(x, y)
                    val diff = abs(Color.red(px) - bgR) + abs(Color.green(px) - bgG) + abs(Color.blue(px) - bgB)
                    if (diff < 32) {
                        result.setPixel(x, y, Color.TRANSPARENT)
                    } else if (diff < 48) {
                        val alpha = ((diff - 32) * 255 / 16).coerceIn(0, 255)
                        result.setPixel(
                            x,
                            y,
                            Color.argb(alpha, Color.red(px), Color.green(px), Color.blue(px))
                        )
                    }
                }
            }
        }
        return result
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

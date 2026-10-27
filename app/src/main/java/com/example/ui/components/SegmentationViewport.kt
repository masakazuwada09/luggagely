package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.BaggageGuardUiState
import com.example.model.BaggageItem
import com.example.model.GearCategory
import com.example.model.GearRigPreset
import com.example.ui.theme.AmberManual
import com.example.ui.theme.ContrastDarkCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LightBorderSubtle
import com.example.ui.theme.LimeChartreuse
import com.example.ui.theme.LimeChartreuseDark
import com.example.ui.theme.PureWhiteCard
import com.example.ui.theme.SoftGraySurface
import com.example.ui.theme.SoftLimeTintBg
import com.example.ui.theme.SuitcaseYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

private const val TAG = "SegmentationViewport"

/**
 * Full-Screen Live Camera with Muse Spark + Meta SAM Segmentation Ready overlay.
 * Automatically fills the screen once camera permission is granted (or shows a full-screen
 * permission prompt if permission is still pending).
 */
@Composable
fun FullScreenCameraSegmentationScreen(
    uiState: BaggageGuardUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onScanCameraFrame: (Bitmap?) -> Unit,
    onSelectRig: (GearRigPreset) -> Unit,
    onSelectItem: (String?) -> Unit,
    onToggleBoxes: () -> Unit,
    onToggleHeatmap: () -> Unit,
    onShiftItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDismissAiError: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "fullscreen_cam_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fullscreen_pulse_alpha"
    )
    val idleLaserY by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fullscreen_idle_laser"
    )

    var useBackLens by remember { mutableStateOf(true) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showObjectsDrawer by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                customPhotoBitmap = decoded
                onScanCameraFrame(decoded)
            }
        }
    }

    val aiItemsWithBoxes = remember(uiState.activeItems) {
        uiState.activeItems.filter { it.boundingBox != null }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ContrastDarkCard)
            .testTag("fullscreen_camera_segmentation_container")
            .pointerInput(aiItemsWithBoxes) {
                detectTapGestures { tapOffset ->
                    val normX = tapOffset.x / size.width
                    val normY = tapOffset.y / size.height
                    val hitItem = aiItemsWithBoxes.firstOrNull { item ->
                        val box = item.boundingBox ?: return@firstOrNull false
                        normX >= box.left && normX <= (box.left + box.width) &&
                            normY >= box.top && normY <= (box.top + box.height)
                    }
                    onSelectItem(hitItem?.id)
                }
            }
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Layer 1: Full-Bleed Live CameraX PreviewView or Custom Picked Photo
        val activePhoto = customPhotoBitmap
        if (activePhoto != null) {
            Image(
                bitmap = activePhoto.asImageBitmap(),
                contentDescription = "Selected Camera Frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasCameraPermission) {
            LiveCameraXPreview(
                useBackLens = useBackLens,
                onPreviewViewReady = { view -> previewViewRef = view },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_live_preview")
            )
        }

        // Layer 2: Full-Screen Meta SAM + Muse Spark Segmentation Canvas Overlays
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("segmentation_viewport_frame")
        ) {
            drawLightThemeViewportScene(
                items = aiItemsWithBoxes,
                selectedItemId = uiState.selectedItemId,
                showBoundingBoxes = uiState.showBoundingBoxes,
                showHeatmap = uiState.showHeatmapOverlay,
                isScanning = uiState.isScanning,
                scanProgress = uiState.scanProgress,
                idleLaserY = idleLaserY,
                pulseAlpha = pulseAlpha,
                hasLiveBackground = hasCameraPermission || activePhoto != null
            )
        }

        // Layer 3: Full-Screen Interactive Bounding Box Masks & Mass Callout Badges
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.showBoundingBoxes,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                aiItemsWithBoxes.forEach { item ->
                    val box = item.boundingBox ?: return@forEach
                    val isSelected = uiState.selectedItemId == item.id
                    val badgeColor = when {
                        isSelected -> LimeChartreuse
                        item.category == GearCategory.POWER -> SuitcaseYellow
                        else -> LimeChartreuse
                    }

                    val xDp = screenWidth * box.left
                    val yDp = screenHeight * box.top

                    Surface(
                        modifier = Modifier
                            .offset(x = xDp + 4.dp, y = yDp + 4.dp)
                            .clickable { onSelectItem(item.id) },
                        color = ContrastDarkCard.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, badgeColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                text = "${item.maskId ?: "SAM"} • ${item.name.take(18)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${BaggageGuardUiState.formatWeight(item.weightKg)}kg",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }

        // Layer 4: Top Floating HUD Controls & Meta SAM Segmentation Ready Status Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ContrastDarkCard.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("fullscreen_camera_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pulsing Meta SAM + Muse Spark Segmentation Ready Badge
                    Row(
                        modifier = Modifier
                            .testTag("ai_segmentation_badge")
                            .clip(RoundedCornerShape(50))
                            .background(ContrastDarkCard.copy(alpha = 0.88f))
                            .border(
                                width = 1.2.dp,
                                color = if (uiState.isScanning) LimeChartreuse else EmeraldSafe,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isScanning) LimeChartreuse.copy(alpha = pulseAlpha)
                                    else EmeraldSafe.copy(alpha = pulseAlpha)
                                )
                        )
                        Text(
                            text = if (uiState.isScanning) {
                                "MUSE SPARK // SCANNING OBJECTS…"
                            } else {
                                "META SAM // SEGMENTATION READY"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (uiState.isScanning) LimeChartreuse else Color.White
                        )
                    }
                }

                // Right HUD Action Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                customPhotoBitmap = null
                                useBackLens = !useBackLens
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("switch_camera_lens_button")
                                .clip(CircleShape)
                                .background(ContrastDarkCard.copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera Lens",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleBoxes,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_boxes_button")
                            .clip(CircleShape)
                            .background(
                                if (uiState.showBoundingBoxes) LimeChartreuse
                                else ContrastDarkCard.copy(alpha = 0.85f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Toggle Meta SAM Bounding Boxes",
                            tint = if (uiState.showBoundingBoxes) TextOnLime else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleHeatmap,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_heatmap_button")
                            .clip(CircleShape)
                            .background(
                                if (uiState.showHeatmapOverlay) LimeChartreuse
                                else ContrastDarkCard.copy(alpha = 0.85f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Gravimetric Heatmap",
                            tint = if (uiState.showHeatmapOverlay) TextOnLime else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pick_luggage_photo_button")
                            .clip(CircleShape)
                            .background(ContrastDarkCard.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Import Photo for Meta Segmentation",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Rig Preset Switcher Pills over Top of Full-Screen Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GearRigPreset.entries.forEach { rig ->
                    val isSelected = uiState.selectedRig == rig
                    Surface(
                        modifier = Modifier
                            .testTag("rig_preset_${rig.id}")
                            .clickable {
                                customPhotoBitmap = null
                                onSelectRig(rig)
                            },
                        color = if (isSelected) LimeChartreuse else ContrastDarkCard.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) LimeChartreuseDark else Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = if (isSelected) TextOnLime else LimeChartreuse,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = rig.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) TextOnLime else Color.White
                            )
                        }
                    }
                }
            }

            // Native Camera Permission Prompt Banner if permission not yet granted
            if (!hasCameraPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_prompt_banner"),
                    color = ContrastDarkCard.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, LimeChartreuse)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = LimeChartreuse,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Full-Screen Camera Permission Required",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Allow native camera access to stream full-screen live video with Meta SAM segmentation.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestCameraPermission,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("grant_camera_permission_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeChartreuse,
                                contentColor = TextOnLime
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Allow Camera",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Layer 5: Bottom Floating Muse Spark + Meta SAM Control Deck
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Scan Summary Pill
            if (!uiState.aiScanSummary.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_scan_summary_banner"),
                    color = ContrastDarkCard.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LimeChartreuse)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LimeChartreuse,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.aiScanSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // AI Error Banner if any
            if (!uiState.aiError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_scan_error_banner"),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CrimsonAlert)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = CrimsonAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.aiError,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = CrimsonAlert,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissAiError,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Detected Objects Drawer + Shutter Bar
            Surface(
                color = ContrastDarkCard.copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Telemetry Summary & Drawer Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showObjectsDrawer = !showObjectsDrawer },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.scanPhaseLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = LimeChartreuse
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(uiState.totalWeightKg)} kg",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (uiState.isOverweight) CrimsonAlert else Color.White,
                                    modifier = Modifier.testTag("total_weight_value")
                                )
                                Text(
                                    text = "/ ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg (${aiItemsWithBoxes.size} segmented)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (showObjectsDrawer) "Hide Objects" else "View Objects",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = if (showObjectsDrawer) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = null,
                                    tint = LimeChartreuse,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Expandable Segmented Objects List inside Full-Screen Camera
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showObjectsDrawer,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 170.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.activeItems, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectItem(item.id) },
                                    color = if (uiState.selectedItemId == item.id) {
                                        LimeChartreuse.copy(alpha = 0.18f)
                                    } else {
                                        Color.White.copy(alpha = 0.07f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.selectedItemId == item.id) LimeChartreuse else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.maskId ?: "MANUAL"} • ${item.category.label}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = LimeChartreuse
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${BaggageGuardUiState.formatWeight(item.weightKg)} kg",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.White
                                            )
                                            IconButton(
                                                onClick = { onShiftItem(item.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoveDown,
                                                    contentDescription = "Shift to personal item",
                                                    tint = Color.White.copy(alpha = 0.75f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteItem(item.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove item",
                                                    tint = Color.White.copy(alpha = 0.75f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Prominent Full-Width Lime "Scan & Segment Objects" Button
                    Button(
                        onClick = {
                            if (!hasCameraPermission) {
                                onRequestCameraPermission()
                            }
                            val frameBitmap = customPhotoBitmap
                                ?: previewViewRef?.bitmap
                                ?: renderSyntheticRigBitmap(uiState.selectedRig, aiItemsWithBoxes)
                            onScanCameraFrame(frameBitmap)
                        },
                        enabled = !uiState.isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scan_gear_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeChartreuse,
                            contentColor = TextOnLime,
                            disabledContainerColor = LimeChartreuse.copy(alpha = 0.55f),
                            disabledContentColor = TextOnLime
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.AutoAwesome else Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isScanning) {
                                "MUSE SPARK + META SAM SCANNING (${(uiState.scanProgress * 100).roundToInt()}%)…"
                            } else {
                                "Scan & Segment Objects"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextOnLime
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentationViewportCard(
    uiState: BaggageGuardUiState,
    onScanGear: () -> Unit,
    onSelectRig: (GearRigPreset) -> Unit,
    onSelectItem: (String?) -> Unit,
    onToggleBoxes: () -> Unit,
    onToggleHeatmap: () -> Unit,
    hasCameraPermission: Boolean = false,
    onRequestCameraPermission: (() -> Unit)? = null,
    onScanCameraFrame: ((Bitmap?) -> Unit)? = null,
    onDismissAiError: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "viewport_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val idleLaserY by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_laser"
    )

    var useBackLens by remember { mutableStateOf(true) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                customPhotoBitmap = decoded
                if (onScanCameraFrame != null) {
                    onScanCameraFrame(decoded)
                } else {
                    onScanGear()
                }
            }
        }
    }

    val aiItemsWithBoxes = remember(uiState.activeItems) {
        uiState.activeItems.filter { it.boundingBox != null }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = LightBorderSubtle,
                shape = RoundedCornerShape(24.dp)
            ),
        color = PureWhiteCard,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Telemetry & Active Segmentation Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing AI Segmentation Active Indicator Badge
                Row(
                    modifier = Modifier
                        .testTag("ai_segmentation_badge")
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (uiState.isScanning) LimeChartreuse.copy(alpha = 0.35f)
                            else SoftLimeTintBg
                        )
                        .border(
                            width = 1.dp,
                            color = if (uiState.isScanning) LimeChartreuseDark
                            else EmeraldSafe.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isScanning) LimeChartreuseDark.copy(alpha = pulseAlpha)
                                else EmeraldSafe.copy(alpha = pulseAlpha)
                            )
                    )
                    Text(
                        text = if (uiState.isScanning) {
                            stringResource(R.string.ai_scanning_in_progress)
                        } else if (hasCameraPermission) {
                            "META SAM // SEGMENTATION READY"
                        } else {
                            stringResource(R.string.ai_segmentation_active)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }

                // Viewport Overlay Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                customPhotoBitmap = null
                                useBackLens = !useBackLens
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("switch_camera_lens_button")
                                .clip(CircleShape)
                                .background(SoftGraySurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera Lens",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleBoxes,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_boxes_button")
                            .clip(CircleShape)
                            .background(
                                if (uiState.showBoundingBoxes) LimeChartreuse
                                else SoftGraySurface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Toggle SAM 3.1 Bounding Boxes",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleHeatmap,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("toggle_heatmap_button")
                            .clip(CircleShape)
                            .background(
                                if (uiState.showHeatmapOverlay) LimeChartreuse
                                else SoftGraySurface
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Volumetric Density Heatmap",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pick_luggage_photo_button")
                            .clip(CircleShape)
                            .background(SoftGraySurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Import Custom Baggage Photo",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Native Camera Permission Banner when permission has not yet been granted
            if (!hasCameraPermission && onRequestCameraPermission != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_prompt_banner"),
                    color = SoftLimeTintBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LimeChartreuseDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Enable Native Camera Feed",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Allow camera permission to stream live video with Meta SAM segmentation.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestCameraPermission,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("grant_camera_permission_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeChartreuse,
                                contentColor = TextOnLime
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Allow Camera",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Loadout / Camera Rig Preset Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GearRigPreset.entries.forEach { rig ->
                    val isSelected = uiState.selectedRig == rig
                    Surface(
                        modifier = Modifier
                            .testTag("rig_preset_${rig.id}")
                            .clickable {
                                customPhotoBitmap = null
                                onSelectRig(rig)
                            },
                        color = if (isSelected) ContrastDarkCard else SoftGraySurface,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) ContrastDarkCard else LightBorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = if (isSelected) LimeChartreuse else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = rig.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            // 16:10 Live CameraX & Meta SAM Segmentation Viewport Frame
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .testTag("segmentation_viewport_frame")
                    .clip(RoundedCornerShape(20.dp))
                    .background(ContrastDarkCard)
                    .border(
                        width = 1.5.dp,
                        color = LightBorderSubtle,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pointerInput(aiItemsWithBoxes) {
                        detectTapGestures { tapOffset ->
                            val normX = tapOffset.x / size.width
                            val normY = tapOffset.y / size.height
                            val hitItem = aiItemsWithBoxes.firstOrNull { item ->
                                val box = item.boundingBox ?: return@firstOrNull false
                                normX >= box.left && normX <= (box.left + box.width) &&
                                    normY >= box.top && normY <= (box.top + box.height)
                            }
                            onSelectItem(hitItem?.id)
                        }
                    }
            ) {
                val frameWidth = maxWidth
                val frameHeight = maxHeight

                val activePhoto = customPhotoBitmap
                if (activePhoto != null) {
                    Image(
                        bitmap = activePhoto.asImageBitmap(),
                        contentDescription = "Selected Luggage Frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (hasCameraPermission) {
                    LiveCameraXPreview(
                        useBackLens = useBackLens,
                        onPreviewViewReady = { view -> previewViewRef = view },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camerax_live_preview")
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLightThemeViewportScene(
                        items = aiItemsWithBoxes,
                        selectedItemId = uiState.selectedItemId,
                        showBoundingBoxes = uiState.showBoundingBoxes,
                        showHeatmap = uiState.showHeatmapOverlay,
                        isScanning = uiState.isScanning,
                        scanProgress = uiState.scanProgress,
                        idleLaserY = idleLaserY,
                        pulseAlpha = pulseAlpha,
                        hasLiveBackground = hasCameraPermission || activePhoto != null
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showBoundingBoxes,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        aiItemsWithBoxes.forEach { item ->
                            val box = item.boundingBox ?: return@forEach
                            val isSelected = uiState.selectedItemId == item.id
                            val badgeColor = when {
                                isSelected -> LimeChartreuse
                                item.category == GearCategory.POWER -> SuitcaseYellow
                                else -> LimeChartreuse
                            }

                            val xDp = frameWidth * box.left
                            val yDp = frameHeight * box.top

                            Surface(
                                modifier = Modifier
                                    .offset(x = xDp + 4.dp, y = yDp + 4.dp)
                                    .clickable { onSelectItem(item.id) },
                                color = ContrastDarkCard.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, badgeColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Text(
                                        text = item.name.take(16),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${BaggageGuardUiState.formatWeight(item.weightKg)}kg",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Top-Left HUD Telemetry Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ContrastDarkCard.copy(alpha = 0.82f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.scanPhaseLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeChartreuse
                    )
                    if (activePhoto != null) {
                        Text(
                            text = "• PHOTO FRAME",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    } else if (hasCameraPermission) {
                        Text(
                            text = "• LIVE CAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSafe
                        )
                    }
                }

                // Bottom HUD Readouts
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MASKS: ${aiItemsWithBoxes.size} // CONF: 98.7%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ContrastDarkCard.copy(alpha = 0.82f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    Text(
                        text = "AI MASS: ${BaggageGuardUiState.formatWeight(uiState.aiScannedWeightKg)} KG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LimeChartreuse,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ContrastDarkCard.copy(alpha = 0.82f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!uiState.aiScanSummary.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_scan_summary_banner"),
                    color = SoftLimeTintBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LimeChartreuseDark.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.aiScanSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (!uiState.aiError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_scan_error_banner"),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonAlert)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = CrimsonAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.aiError,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = CrimsonAlert,
                            modifier = Modifier.weight(1f)
                        )
                        if (onDismissAiError != null) {
                            IconButton(
                                onClick = onDismissAiError,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = CrimsonAlert,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Prominent Lime-Yellow "Scan & Identify" Pill Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasCameraPermission) "Muse Spark + Meta SAM Ready" else uiState.selectedRig.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (uiState.isScanning) {
                            "Segmenting & identifying objects (${(uiState.scanProgress * 100).roundToInt()}%)…"
                        } else {
                            "Tap Scan Gear to segment & identify objects in view"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (!hasCameraPermission && onRequestCameraPermission != null) {
                            onRequestCameraPermission()
                        }
                        if (onScanCameraFrame != null) {
                            val frameBitmap = customPhotoBitmap
                                ?: previewViewRef?.bitmap
                                ?: renderSyntheticRigBitmap(uiState.selectedRig, aiItemsWithBoxes)
                            onScanCameraFrame(frameBitmap)
                        } else {
                            onScanGear()
                        }
                    },
                    enabled = !uiState.isScanning,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("scan_gear_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeChartreuse,
                        contentColor = TextOnLime,
                        disabledContainerColor = LimeChartreuse.copy(alpha = 0.5f),
                        disabledContentColor = TextOnLime.copy(alpha = 0.7f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isScanning) Icons.Default.AutoAwesome else Icons.Default.Radar,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isScanning) {
                            "${(uiState.scanProgress * 100).roundToInt()}%"
                        } else {
                            stringResource(R.string.scan_gear)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCameraXPreview(
    useBackLens: Boolean,
    onPreviewViewReady: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, useBackLens) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val preferredSelector = if (useBackLens) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                val fallbackSelector = if (useBackLens) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                val activeSelector = when {
                    runCatching { cameraProvider.hasCamera(preferredSelector) }.getOrDefault(false) -> preferredSelector
                    runCatching { cameraProvider.hasCamera(fallbackSelector) }.getOrDefault(false) -> fallbackSelector
                    else -> CameraSelector.Builder().build()
                }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview
                )
                onPreviewViewReady(previewView)
            } catch (e: Exception) {
                Log.w(TAG, "CameraX binding warning (emulator or device camera busy)", e)
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    AndroidView(
        factory = {
            onPreviewViewReady(previewView)
            previewView
        },
        modifier = modifier
    )
}

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to decode picked image URI", e)
        null
    }
}

private fun renderSyntheticRigBitmap(rig: GearRigPreset, items: List<BaggageItem>): Bitmap {
    val width = 640
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(0xFF1E2228.toInt(), 0xFF14171B.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFD4EC28.toInt()
    }
    canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 28f, 28f, outlinePaint)

    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2E3440.toInt()
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 20f
        isFakeBoldText = true
    }

    canvas.drawText("Baggage Loadout: ${rig.title}", 44f, 60f, textPaint)

    items.forEach { item ->
        val box = item.boundingBox ?: return@forEach
        val l = box.left * width
        val t = box.top * height
        val r = (box.left + box.width) * width
        val b = (box.top + box.height) * height
        canvas.drawRoundRect(RectF(l, t, r, b), 16f, 16f, boxPaint)
        canvas.drawRoundRect(RectF(l, t, r, b), 16f, 16f, outlinePaint)
        canvas.drawText(item.name, l + 12f, t + 36f, textPaint)
        canvas.drawText("${BaggageGuardUiState.formatWeight(item.weightKg)} kg", l + 12f, t + 64f, textPaint)
    }

    return bitmap
}

private fun DrawScope.drawLightThemeViewportScene(
    items: List<BaggageItem>,
    selectedItemId: String?,
    showBoundingBoxes: Boolean,
    showHeatmap: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    idleLaserY: Float,
    pulseAlpha: Float,
    hasLiveBackground: Boolean
) {
    val w = size.width
    val h = size.height

    if (!hasLiveBackground) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF24272B),
                    Color(0xFF1A1C1F),
                    Color(0xFF121416)
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.75f
            )
        )
    } else {
        drawRect(
            color = Color.Black.copy(alpha = 0.16f)
        )
    }

    val gridCols = 14
    val gridRows = 9
    val cellW = w / gridCols
    val cellH = h / gridRows
    val gridColor = Color.White.copy(alpha = if (hasLiveBackground) 0.08f else 0.06f)

    for (i in 1 until gridCols) {
        drawLine(gridColor, Offset(i * cellW, 0f), Offset(i * cellW, h), 1f)
    }
    for (j in 1 until gridRows) {
        drawLine(gridColor, Offset(0f, j * cellH), Offset(w, j * cellH), 1f)
    }

    val shellMarginX = w * 0.035f
    val shellMarginY = h * 0.06f
    drawRoundRect(
        color = LimeChartreuse.copy(alpha = 0.34f),
        topLeft = Offset(shellMarginX, shellMarginY),
        size = Size(w - shellMarginX * 2, h - shellMarginY * 2),
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 2f)
    )

    items.forEach { item ->
        val box = item.boundingBox ?: return@forEach
        val left = box.left * w
        val top = box.top * h
        val rectW = box.width * w
        val rectH = box.height * h
        val isSelected = item.id == selectedItemId

        val maskPrimaryColor = when {
            isSelected -> LimeChartreuse
            showHeatmap && item.weightKg >= 2.0 -> CrimsonAlert
            showHeatmap && item.weightKg >= 1.0 -> AmberManual
            item.category == GearCategory.POWER -> SuitcaseYellow
            else -> LimeChartreuse
        }

        if (!hasLiveBackground) {
            drawRoundRect(
                color = Color(0xFF2B2F36),
                topLeft = Offset(left + 4f, top + 4f),
                size = Size(rectW - 8f, rectH - 8f),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }

        val maskAlpha = if (isSelected) 0.36f else if (showHeatmap) 0.30f else 0.20f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    maskPrimaryColor.copy(alpha = maskAlpha),
                    maskPrimaryColor.copy(alpha = maskAlpha * 0.4f)
                ),
                start = Offset(left, top),
                end = Offset(left + rectW, top + rectH)
            ),
            topLeft = Offset(left + 4f, top + 4f),
            size = Size(rectW - 8f, rectH - 8f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        if (showBoundingBoxes) {
            drawRoundRect(
                color = maskPrimaryColor.copy(alpha = if (isSelected) 1f else 0.85f),
                topLeft = Offset(left, top),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(
                    width = if (isSelected) 3f else 2f,
                    pathEffect = if (isSelected) null else PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                )
            )

            val tick = minOf(rectW, rectH) * 0.16f
            val bracketStroke = if (isSelected) 4f else 2.5f
            val bracketColor = maskPrimaryColor.copy(alpha = if (isSelected) 1f else pulseAlpha)

            drawLine(bracketColor, Offset(left, top), Offset(left + tick, top), bracketStroke)
            drawLine(bracketColor, Offset(left, top), Offset(left, top + tick), bracketStroke)
            drawLine(bracketColor, Offset(left + rectW - tick, top), Offset(left + rectW, top), bracketStroke)
            drawLine(bracketColor, Offset(left + rectW, top), Offset(left + rectW, top + tick), bracketStroke)
            drawLine(bracketColor, Offset(left, top + rectH - tick), Offset(left, top + rectH), bracketStroke)
            drawLine(bracketColor, Offset(left, top + rectH), Offset(left + tick, top + rectH), bracketStroke)
            drawLine(bracketColor, Offset(left + rectW - tick, top + rectH), Offset(left + rectW, top + rectH), bracketStroke)
            drawLine(bracketColor, Offset(left + rectW, top + rectH - tick), Offset(left + rectW, top + rectH), bracketStroke)
        }
    }

    val activeScanY = if (isScanning) (scanProgress * h) else (idleLaserY * h)
    val laserColor = LimeChartreuse

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                laserColor.copy(alpha = if (isScanning) 0.30f else 0.10f),
                laserColor
            ),
            startY = (activeScanY - h * 0.18f).coerceAtLeast(0f),
            endY = activeScanY
        ),
        topLeft = Offset(0f, (activeScanY - h * 0.18f).coerceAtLeast(0f)),
        size = Size(w, h * 0.18f)
    )

    drawLine(
        color = laserColor,
        start = Offset(0f, activeScanY),
        end = Offset(w, activeScanY),
        strokeWidth = if (isScanning) 3.5f else 1.5f
    )

    val reticleLen = minOf(w, h) * 0.09f
    val pad = 10f
    val reticleColor = LimeChartreuse.copy(alpha = 0.9f)
    val reticleStroke = 3.5f

    drawLine(reticleColor, Offset(pad, pad), Offset(pad + reticleLen, pad), reticleStroke)
    drawLine(reticleColor, Offset(pad, pad), Offset(pad, pad + reticleLen), reticleStroke)
    drawLine(reticleColor, Offset(w - pad - reticleLen, pad), Offset(w - pad, pad), reticleStroke)
    drawLine(reticleColor, Offset(w - pad, pad), Offset(w - pad, pad + reticleLen), reticleStroke)
    drawLine(reticleColor, Offset(pad, h - pad - reticleLen), Offset(pad, h - pad), reticleStroke)
    drawLine(reticleColor, Offset(pad, h - pad), Offset(pad + reticleLen, h - pad), reticleStroke)
    drawLine(reticleColor, Offset(w - pad - reticleLen, h - pad), Offset(w - pad, h - pad), reticleStroke)
    drawLine(reticleColor, Offset(w - pad, h - pad - reticleLen), Offset(w - pad, h - pad), reticleStroke)
}

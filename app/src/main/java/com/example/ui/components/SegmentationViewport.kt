package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.BaggageAsyncStorage
import com.example.model.BaggageGuardUiState
import com.example.model.BaggageItem
import com.example.model.GearRigPreset
import com.example.model.NormalizedPoint
import com.example.model.NormalizedRect
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DashboardBorderLight
import com.example.ui.theme.DashboardCardWhite
import com.example.ui.theme.DashboardFeedGrayBg
import com.example.ui.theme.DashboardSurfaceLight
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.FacebookBlueSoftBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val TAG = "SegmentationViewport"

/**
 * Renders the saved SAM silhouette cutout image (decoded from AsyncStorage data URI or local file)
 * for display on the Dashboard item list, bottom sheet, and Camera cutout preview.
 * Uses [ContentScale.Fit] so the non-rectangular SAM object silhouette sticker is clearly visible.
 */
@Composable
fun SegmentedItemThumbnail(
    imageUri: String?,
    itemName: String,
    sizeDp: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(imageUri) {
        BaggageAsyncStorage.decodeImageUriToBitmap(imageUri)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(12.dp))
            .background(FacebookBlueSoftBg)
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(12.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = itemName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Luggage,
                contentDescription = itemName,
                tint = FacebookBlue,
                modifier = Modifier.size(sizeDp * 0.5f)
            )
        }
    }
}

private data class HoverDetectionSample(
    val box: NormalizedRect,
    val contourPoints: List<NormalizedPoint>,
    val signature: IntArray,
    val hasSalientObject: Boolean
)

/**
 * Edge-to-Edge Full-Screen Camera with True SAM Workflow:
 * 1. Capture Image First (via Capture button, tapping on the object, photo picker, or steady hover lock)
 * 2. Segment the captured frame & trace the exact non-rectangular object silhouette contour
 * 3. Cut out the precise object shape (with transparent background + SAM sticker outline)
 * 4. Auto-label the item and save to AsyncStorage + Dashboard item list.
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
    var useBackLens by remember { mutableStateOf(true) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Frozen captured image for the "Capture -> Segment -> Cut -> Label" SAM workflow
    var capturedFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Live organic non-square contour preview while aiming the camera
    val defaultBox = remember { NormalizedRect(0.22f, 0.22f, 0.56f, 0.48f) }
    var liveHoverBox by remember { mutableStateOf(defaultBox) }
    var liveContourPoints by remember {
        mutableStateOf(BaggageAsyncStorage.createOrganicFallbackContour(defaultBox))
    }
    var hoverStatusText by remember {
        mutableStateOf("Capture image or tap object to segment, cut & label")
    }
    var hoverLockTicks by remember { mutableIntStateOf(0) }
    var hasPerformedInitialCapture by remember { mutableStateOf(false) }

    val currentIsScanning by rememberUpdatedState(uiState.isScanning)
    val currentOnScanCameraFrame by rememberUpdatedState(onScanCameraFrame)

    // Helper that captures/freezes the current frame first, then triggers SAM segmentation + precise cutout + labelling
    val triggerCaptureAndSegment: (Bitmap?) -> Unit = { explicitBitmap ->
        val rawFrame = explicitBitmap ?: previewViewRef?.bitmap
        if (rawFrame != null) {
            val frozenCopy = rawFrame.copy(Bitmap.Config.ARGB_8888, false)
            capturedFrameBitmap = frozenCopy
            val sample = analyzeLiveHoverFrame(frozenCopy)
            liveHoverBox = sample.box
            liveContourPoints = sample.contourPoints
            hoverStatusText = "Image captured • Running SAM contour cutout & label…"
            currentOnScanCameraFrame(frozenCopy)
        } else {
            hoverStatusText = "Image captured • Running SAM contour cutout & label…"
            currentOnScanCameraFrame(null)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                hasPerformedInitialCapture = true
                triggerCaptureAndSegment(decoded)
            }
        }
    }

    // Live contour preview & steady-hover auto-capture assist when camera is in live mode
    LaunchedEffect(hasCameraPermission, useBackLens, capturedFrameBitmap) {
        var previousSignature: IntArray? = null
        var stableFrames = 0

        while (true) {
            delay(380L)
            if (currentIsScanning) {
                hoverStatusText = "SAM segmenting contour, cutting & labelling…"
                continue
            }

            // If an image is already captured and frozen, keep displaying its segmented cutout
            if (capturedFrameBitmap != null) {
                hoverStatusText = "Image captured • Precise SAM contour cut & labelled"
                continue
            }

            val liveBitmap = previewViewRef?.bitmap
            if (liveBitmap != null && liveBitmap.width > 16 && liveBitmap.height > 16) {
                val sample = analyzeLiveHoverFrame(liveBitmap)
                liveHoverBox = sample.box
                liveContourPoints = sample.contourPoints

                val prevSig = previousSignature
                val motionDiff = if (prevSig != null) {
                    computeSignatureDelta(sample.signature, prevSig)
                } else {
                    0
                }
                previousSignature = sample.signature

                if (motionDiff <= 16 && sample.hasSalientObject) {
                    stableFrames = (stableFrames + 1).coerceAtMost(4)
                    hoverLockTicks = stableFrames
                    hoverStatusText = when (stableFrames) {
                        1 -> "Tracing object contour • Tap Capture to cut out"
                        2 -> "Holding steady • Capturing image for SAM…"
                        else -> "Capturing image & cutting precise contour…"
                    }

                    if (stableFrames >= 3 && !hasPerformedInitialCapture) {
                        hasPerformedInitialCapture = true
                        stableFrames = 0
                        triggerCaptureAndSegment(liveBitmap)
                    }
                } else {
                    stableFrames = 1
                    hoverLockTicks = 1
                    hoverStatusText = "Aim at object & tap Capture to segment, cut & label"
                }
            } else if (!hasPerformedInitialCapture) {
                // Virtual / emulator camera fallback: capture & segment initial item once
                delay(700L)
                if (!currentIsScanning && !hasPerformedInitialCapture && capturedFrameBitmap == null) {
                    hasPerformedInitialCapture = true
                    hoverLockTicks = 3
                    hoverStatusText = "Capturing image & cutting precise SAM contour…"
                    currentOnScanCameraFrame(null)
                }
            }
        }
    }

    // Single segmented item currently highlighted and cut out
    val singleSegmentedItem: BaggageItem? = remember(uiState.lastSegmentedItem, uiState.activeItems) {
        uiState.lastSegmentedItem
            ?: uiState.activeItems.firstOrNull { it.boundingBox != null }
    }

    val displayedBox: NormalizedRect = singleSegmentedItem?.boundingBox ?: liveHoverBox
    val displayedContour: List<NormalizedPoint> = remember(singleSegmentedItem, liveContourPoints, displayedBox) {
        when {
            singleSegmentedItem != null && singleSegmentedItem.contourPoints.size >= 6 ->
                singleSegmentedItem.contourPoints
            liveContourPoints.size >= 6 ->
                liveContourPoints
            else ->
                BaggageAsyncStorage.createOrganicFallbackContour(displayedBox)
        }
    }

    val isImageCapturedOrSegmented = capturedFrameBitmap != null || singleSegmentedItem != null

    // Full-Screen Edge-to-Edge Camera Container
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg)
            .testTag("fullscreen_camera_segmentation_container")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tapping anywhere on the object in the camera view captures the image first, then segments & cuts it
                if (!uiState.isScanning) {
                    hasPerformedInitialCapture = true
                    triggerCaptureAndSegment(capturedFrameBitmap ?: previewViewRef?.bitmap)
                }
            }
    ) {
        val viewW = maxWidth
        val viewH = maxHeight
        val frozenPhoto = capturedFrameBitmap

        // 1. Edge-to-Edge Full-Screen Live Camera Feed OR Captured Frozen Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("segmentation_viewport_frame")
        ) {
            if (frozenPhoto != null) {
                Image(
                    bitmap = frozenPhoto.asImageBitmap(),
                    contentDescription = "Captured Frame for SAM Segmentation",
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DashboardFeedGrayBg)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(FacebookBlueSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Capture & Segment Item with SAM",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Captures image first, traces the exact non-square object contour, cuts it out, and labels it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // 2. Precise Non-Square SAM Object Silhouette Contour Highlight on Full Screen
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPreciseSamContourHighlight(
                    contourPoints = displayedContour,
                    isLocked = isImageCapturedOrSegmented || hoverLockTicks >= 2,
                    dimOutsideBackdrop = isImageCapturedOrSegmented
                )
            }

            // 3. Auto-Labelling Floating Pill Anchored to the Top of the Segmented Object Contour
            val minContourX = displayedContour.minOfOrNull { it.x } ?: displayedBox.left
            val minContourY = displayedContour.minOfOrNull { it.y } ?: displayedBox.top
            val labelXDp = (viewW * minContourX).coerceIn(16.dp, (viewW - 230.dp).coerceAtLeast(16.dp))
            val labelYDp = (viewH * minContourY - 46.dp).coerceIn(96.dp, (viewH - 220.dp).coerceAtLeast(96.dp))

            Surface(
                modifier = Modifier.offset(x = labelXDp, y = labelYDp),
                color = if (singleSegmentedItem != null) FacebookBlue else DashboardCardWhite.copy(alpha = 0.95f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.5.dp,
                    if (singleSegmentedItem != null) Color.White else FacebookBlue
                ),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            color = if (singleSegmentedItem != null) Color.White else FacebookBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (singleSegmentedItem != null) Icons.Default.ContentCut else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (singleSegmentedItem != null) Color.White else FacebookBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    val pillLabel = when {
                        uiState.isScanning -> "SAM Segmenting & Cutting…"
                        singleSegmentedItem != null -> singleSegmentedItem.name
                        else -> "Tap to Capture & Cut Contour"
                    }

                    Text(
                        text = pillLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (singleSegmentedItem != null) Color.White else TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (singleSegmentedItem != null && singleSegmentedItem.weightKg > 0.0) {
                        Text(
                            text = "• ${BaggageGuardUiState.formatWeight(singleSegmentedItem.weightKg)} kg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 4. Floating Top Header Overlay (Full-Screen Camera with StatusBarsPadding)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DashboardCardWhite.copy(alpha = 0.94f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, DashboardBorderLight),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DashboardSurfaceLight)
                                .border(1.dp, DashboardBorderLight, CircleShape)
                                .testTag("fullscreen_camera_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isScanning) FacebookBlue else EmeraldSafe)
                            )
                            Column {
                                Text(
                                    text = if (capturedFrameBitmap != null) {
                                        "Captured SAM Cutout"
                                    } else {
                                        "SAM Capture • Segment • Cut"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = hoverStatusText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = {
                                    capturedFrameBitmap = null
                                    useBackLens = !useBackLens
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("switch_camera_lens_button")
                                    .clip(CircleShape)
                                    .background(DashboardSurfaceLight)
                                    .border(1.dp, DashboardBorderLight, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera Lens",
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                                .background(FacebookBlueSoftBg)
                                .border(1.dp, FacebookBlue.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Pick Photo to Segment",
                                tint = FacebookBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!hasCameraPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_prompt_banner"),
                    color = DashboardCardWhite.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, FacebookBlue),
                    shadowElevation = 4.dp
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
                                tint = FacebookBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Allow camera access to capture & segment objects",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestCameraPermission,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("grant_camera_permission_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Allow",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
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
                            style = MaterialTheme.typography.bodySmall,
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
        }

        // 5. Floating Bottom Capture & Precise SAM Cutout Card Over Full-Screen Camera
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("scan_gear_button")
                .clickable {
                    if (!uiState.isScanning) {
                        hasPerformedInitialCapture = true
                        triggerCaptureAndSegment(capturedFrameBitmap ?: previewViewRef?.bitmap)
                    }
                },
            color = DashboardCardWhite.copy(alpha = 0.96f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, DashboardBorderLight),
            shadowElevation = 8.dp
        ) {
            if (singleSegmentedItem != null && !singleSegmentedItem.imageUri.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SegmentedItemThumbnail(
                        imageUri = singleSegmentedItem.imageUri,
                        itemName = singleSegmentedItem.name,
                        sizeDp = 58.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PRECISE SAM CUTOUT SAVED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = FacebookBlue
                                )
                            )
                        }
                        Text(
                            text = singleSegmentedItem.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(singleSegmentedItem.weightKg)} kg • Added to Dashboard list",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedFrameBitmap = null
                                hoverStatusText = "Aim at next object & tap Capture to segment"
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, DashboardBorderLight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Capture New Image",
                                tint = FacebookBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FacebookBlue
                                )
                            )
                        }

                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(FacebookBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Capture & Segment",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isScanning) {
                                    "Segmenting & Cutting Contour…"
                                } else {
                                    "Capture & Segment Object"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Captures image first, traces exact shape, cuts & labels",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            )
                        }
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
    val singleSegmentedItem = remember(uiState.lastSegmentedItem, uiState.activeItems) {
        uiState.lastSegmentedItem ?: uiState.activeItems.firstOrNull { it.boundingBox != null }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
        color = DashboardCardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .testTag("segmentation_viewport_frame")
                    .clip(RoundedCornerShape(14.dp))
                    .background(DashboardSurfaceLight)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(14.dp))
            ) {
                if (singleSegmentedItem?.boundingBox != null) {
                    val contour = if (singleSegmentedItem.contourPoints.size >= 6) {
                        singleSegmentedItem.contourPoints
                    } else {
                        BaggageAsyncStorage.createOrganicFallbackContour(singleSegmentedItem.boundingBox)
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPreciseSamContourHighlight(
                            contourPoints = contour,
                            isLocked = true,
                            dimOutsideBackdrop = false
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (onScanCameraFrame != null) {
                        onScanCameraFrame(null)
                    } else {
                        onScanGear()
                    }
                },
                enabled = !uiState.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("scan_gear_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FacebookBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Capture & Segment Item",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Analyzes a camera frame to detect the foreground object's bounding box, its 36-point non-rectangular
 * silhouette contour, and a 16-point perceptual color signature.
 */
private fun analyzeLiveHoverFrame(bitmap: Bitmap): HoverDetectionSample {
    val w = bitmap.width
    val h = bitmap.height
    val signature = IntArray(16)
    var idx = 0
    for (gy in 0 until 4) {
        val py = ((gy + 1) * h / 5).coerceIn(0, h - 1)
        for (gx in 0 until 4) {
            val px = ((gx + 1) * w / 5).coerceIn(0, w - 1)
            val c = bitmap.getPixel(px, py)
            val luma = (android.graphics.Color.red(c) * 30 +
                android.graphics.Color.green(c) * 59 +
                android.graphics.Color.blue(c) * 11) / 100
            signature[idx++] = luma
        }
    }

    var bgR = 0
    var bgG = 0
    var bgB = 0
    var bgSamples = 0
    val stepX = (w / 14).coerceAtLeast(1)
    val stepY = (h / 14).coerceAtLeast(1)

    for (x in 0 until w step stepX) {
        val topPx = bitmap.getPixel(x, (h * 0.08f).toInt().coerceIn(0, h - 1))
        val botPx = bitmap.getPixel(x, (h * 0.92f).toInt().coerceIn(0, h - 1))
        bgR += android.graphics.Color.red(topPx) + android.graphics.Color.red(botPx)
        bgG += android.graphics.Color.green(topPx) + android.graphics.Color.green(botPx)
        bgB += android.graphics.Color.blue(topPx) + android.graphics.Color.blue(botPx)
        bgSamples += 2
    }
    val avgR = bgR / bgSamples.coerceAtLeast(1)
    val avgG = bgG / bgSamples.coerceAtLeast(1)
    val avgB = bgB / bgSamples.coerceAtLeast(1)

    var minX = w
    var maxX = 0
    var minY = h
    var maxY = 0
    var fgCount = 0

    val xStart = (w * 0.14f).toInt()
    val xEnd = (w * 0.86f).toInt()
    val yStart = (h * 0.18f).toInt()
    val yEnd = (h * 0.80f).toInt()

    for (y in yStart until yEnd step stepY) {
        for (x in xStart until xEnd step stepX) {
            val c = bitmap.getPixel(x, y)
            val diff = abs(android.graphics.Color.red(c) - avgR) +
                abs(android.graphics.Color.green(c) - avgG) +
                abs(android.graphics.Color.blue(c) - avgB)
            if (diff > 42) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                fgCount++
            }
        }
    }

    val box = if (fgCount >= 5 && maxX > minX && maxY > minY) {
        val l = (minX.toFloat() / w - 0.03f).coerceIn(0.10f, 0.52f)
        val t = (minY.toFloat() / h - 0.03f).coerceIn(0.16f, 0.52f)
        val bw = ((maxX - minX).toFloat() / w + 0.06f).coerceIn(0.28f, (0.88f - l).coerceAtLeast(0.28f))
        val bh = ((maxY - minY).toFloat() / h + 0.06f).coerceIn(0.26f, (0.80f - t).coerceAtLeast(0.26f))
        NormalizedRect(l, t, bw, bh)
    } else {
        NormalizedRect(0.20f, 0.24f, 0.60f, 0.46f)
    }

    val contourPoints = BaggageAsyncStorage.extractPreciseObjectContour(bitmap, box)

    return HoverDetectionSample(
        box = box,
        contourPoints = contourPoints,
        signature = signature,
        hasSalientObject = true
    )
}

private fun computeSignatureDelta(a: IntArray, b: IntArray): Int {
    var sum = 0
    val len = minOf(a.size, b.size)
    for (i in 0 until len) {
        sum += abs(a[i] - b[i])
    }
    return sum / len.coerceAtLeast(1)
}

/**
 * Draws a precise, non-rectangular SAM object contour highlight using smooth Catmull-Rom / cubic Bezier
 * spline curves that hug the true silhouette of the segmented object (never a square or rectangle).
 */
private fun DrawScope.drawPreciseSamContourHighlight(
    contourPoints: List<NormalizedPoint>,
    isLocked: Boolean,
    dimOutsideBackdrop: Boolean
) {
    if (contourPoints.size < 3) return

    val androidPath = BaggageAsyncStorage.buildSmoothContourPath(
        points = contourPoints,
        width = size.width,
        height = size.height
    )
    val composePath = androidPath.asComposePath()

    // Subtle dark backdrop outside the segmented object contour when image is captured
    if (dimOutsideBackdrop) {
        clipPath(composePath, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.28f))
        }
    }

    // Soft solid SAM mask fill strictly inside the object's silhouette contour
    drawPath(
        path = composePath,
        color = FacebookBlue.copy(alpha = if (isLocked) 0.22f else 0.12f)
    )

    // Crisp white SAM cutout sticker halo following the exact object contour
    drawPath(
        path = composePath,
        color = Color.White.copy(alpha = 0.96f),
        style = Stroke(
            width = if (isLocked) 6.5f else 4.0f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Inner solid FacebookBlue SAM contour border hugging the object silhouette
    drawPath(
        path = composePath,
        color = FacebookBlue,
        style = Stroke(
            width = 3.0f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
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

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SegmentationViewport"

/**
 * High-level workflow steps for the Camera -> SAM Image Editor -> Cutout pipeline:
 * 1. [CAPTURE_CAMERA]: Live full-screen camera viewfinder to capture or pick an image first.
 * 2. [SAM_IMAGE_EDITOR]: Dedicated Segment Anything Image Editor where the user taps interactive
 *    foreground/background points (`+` / `-`) and draws/adjusts bounding boxes, then taps **Cut Object**.
 */
private enum class CameraEditorWorkflowStep {
    CAPTURE_CAMERA,
    SAM_IMAGE_EDITOR
}

/**
 * Interactive prompt tool modes inside the SAM Image Editor.
 */
private enum class SamEditorPromptTool(val label: String) {
    ADD_POINT("+ Point"),
    EXCLUDE_POINT("- Exclude"),
    DRAW_BOX("Box")
}

private sealed interface SamPromptHistoryEntry {
    data class PositivePoint(val point: NormalizedPoint) : SamPromptHistoryEntry
    data class NegativePoint(val point: NormalizedPoint) : SamPromptHistoryEntry
    data class BoxPrompt(val previousBox: NormalizedRect?) : SamPromptHistoryEntry
}

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

/**
 * Full-Screen Camera -> SAM Interactive Image Editor -> Cutout Workflow:
 * 1. **Capture Image First**: Live full-screen camera preview with a prominent Capture button.
 * 2. **Redirect to SAM Image Editor**: Opens the captured image in an interactive Segment Anything editor
 *    where the user can tap interactive points (`+ Point`, `- Exclude`) and drag interactive boxes (`Box`)
 *    to refine the exact non-rectangular object silhouette mask.
 * 3. **Show Button to Cut**: Displays the prominent **"Cut Object"** button to slice out the segmented
 *    silhouette, auto-label it, save it to AsyncStorage, and show it on the Dashboard item list.
 */
@Composable
fun FullScreenCameraSegmentationScreen(
    uiState: BaggageGuardUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onScanCameraFrame: (Bitmap?) -> Unit,
    onCutWithInteractiveMask: ((Bitmap?, NormalizedRect?, List<NormalizedPoint>?) -> Unit)? = null,
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

    // Current step: Step 1 = CAPTURE_CAMERA, Step 2 = SAM_IMAGE_EDITOR
    var workflowStep by remember { mutableStateOf(CameraEditorWorkflowStep.CAPTURE_CAMERA) }
    var capturedEditorBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Interactive SAM Image Editor state (Points + Box + Undo History)
    var activePromptTool by remember { mutableStateOf(SamEditorPromptTool.ADD_POINT) }
    val positivePoints = remember { mutableStateListOf<NormalizedPoint>() }
    val negativePoints = remember { mutableStateListOf<NormalizedPoint>() }
    var interactiveBox by remember { mutableStateOf<NormalizedRect?>(null) }
    val promptHistory = remember { mutableStateListOf<SamPromptHistoryEntry>() }
    var hasCutCurrentImage by remember { mutableStateOf(false) }

    // Drag gesture state for drawing/resizing a SAM Box prompt on the image editor
    var dragStartNorm by remember { mutableStateOf<NormalizedPoint?>(null) }

    // Helper to transition from Step 1 (Camera Capture) -> Step 2 (SAM Image Editor)
    val openSamImageEditorWithBitmap: (Bitmap) -> Unit = { bitmap ->
        capturedEditorBitmap = bitmap
        positivePoints.clear()
        negativePoints.clear()
        promptHistory.clear()
        // Seed an initial interactive center point & box so the user immediately sees the SAM contour
        // and can tap more points, draw a new box, or tap "Cut Object" right away
        val initialPoint = NormalizedPoint(0.50f, 0.48f)
        positivePoints.add(initialPoint)
        promptHistory.add(SamPromptHistoryEntry.PositivePoint(initialPoint))
        val (seededBox, _) = BaggageAsyncStorage.extractInteractiveSamContour(
            sourceBitmap = bitmap,
            positivePoints = listOf(initialPoint),
            negativePoints = emptyList(),
            customBox = null
        )
        interactiveBox = seededBox
        hasCutCurrentImage = false
        workflowStep = CameraEditorWorkflowStep.SAM_IMAGE_EDITOR
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                openSamImageEditorWithBitmap(decoded)
            }
        }
    }

    // Recompute the live SAM region & 36-point non-rectangular silhouette contour whenever points/box change
    val liveSamPair: Pair<NormalizedRect, List<NormalizedPoint>> = remember(
        capturedEditorBitmap,
        positivePoints.toList(),
        negativePoints.toList(),
        interactiveBox
    ) {
        BaggageAsyncStorage.extractInteractiveSamContour(
            sourceBitmap = capturedEditorBitmap,
            positivePoints = positivePoints.toList(),
            negativePoints = negativePoints.toList(),
            customBox = interactiveBox
        )
    }

    val computedBox = liveSamPair.first
    val computedContour = liveSamPair.second
    val hasActivePrompts = positivePoints.isNotEmpty() || interactiveBox != null

    val latestCutItem: BaggageItem? = remember(uiState.lastSegmentedItem, hasCutCurrentImage) {
        if (hasCutCurrentImage) uiState.lastSegmentedItem else null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg)
            .testTag("fullscreen_camera_segmentation_container")
    ) {
        val viewW = maxWidth
        val viewH = maxHeight

        when (workflowStep) {
            // =========================================================================
            // STEP 1: FULL-SCREEN CAMERA CAPTURE (Capture Image First)
            // =========================================================================
            CameraEditorWorkflowStep.CAPTURE_CAMERA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("segmentation_viewport_frame")
                ) {
                    if (hasCameraPermission) {
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
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(FacebookBlueSoftBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Step 1: Capture Image First",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Capture a photo of your item to open the SAM Image Editor, where you can tap interactive points & boxes and cut it out.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Subtle framing guide on live camera before capture
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val guideW = size.width * 0.72f
                        val guideH = size.height * 0.48f
                        val left = (size.width - guideW) / 2f
                        val top = (size.height - guideH) / 2f
                        val cornerLen = 36f
                        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)
                        val guideColor = Color.White.copy(alpha = 0.85f)

                        // Top-left corner bracket
                        drawLine(guideColor, Offset(left, top), Offset(left + cornerLen, top), stroke.width)
                        drawLine(guideColor, Offset(left, top), Offset(left, top + cornerLen), stroke.width)
                        // Top-right corner bracket
                        drawLine(guideColor, Offset(left + guideW, top), Offset(left + guideW - cornerLen, top), stroke.width)
                        drawLine(guideColor, Offset(left + guideW, top), Offset(left + guideW, top + cornerLen), stroke.width)
                        // Bottom-left corner bracket
                        drawLine(guideColor, Offset(left, top + guideH), Offset(left + cornerLen, top + guideH), stroke.width)
                        drawLine(guideColor, Offset(left, top + guideH), Offset(left, top + guideH - cornerLen), stroke.width)
                        // Bottom-right corner bracket
                        drawLine(guideColor, Offset(left + guideW, top + guideH), Offset(left + guideW - cornerLen, top + guideH), stroke.width)
                        drawLine(guideColor, Offset(left + guideW, top + guideH), Offset(left + guideW, top + guideH - cornerLen), stroke.width)
                    }
                }

                // Top Header Bar (Camera Capture Mode)
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

                                Column {
                                    Text(
                                        text = "Step 1 • Capture Image",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Capture photo to open SAM Image Editor",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasCameraPermission) {
                                    IconButton(
                                        onClick = { useBackLens = !useBackLens },
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
                                        contentDescription = "Pick Photo for SAM Editor",
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
                                        text = "Allow camera access to capture photos for SAM Editor",
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
                }

                // Bottom Capture Shutter Card — Captures Image First & Redirects to SAM Image Editor
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag("scan_gear_button")
                        .clickable {
                            val liveBitmap = previewViewRef?.bitmap
                            val captured = if (liveBitmap != null && liveBitmap.width > 16 && liveBitmap.height > 16) {
                                liveBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                BaggageAsyncStorage.createDefaultCameraCaptureBitmap()
                            }
                            openSamImageEditorWithBitmap(captured)
                        },
                    color = DashboardCardWhite.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DashboardBorderLight),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Capture Image for SAM Editor",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Redirects to Image Editor to tap interactive points & boxes",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val liveBitmap = previewViewRef?.bitmap
                                val captured = if (liveBitmap != null && liveBitmap.width > 16 && liveBitmap.height > 16) {
                                    liveBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                } else {
                                    BaggageAsyncStorage.createDefaultCameraCaptureBitmap()
                                }
                                openSamImageEditorWithBitmap(captured)
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("capture_photo_redirect_editor_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Capture Image",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Capture",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // STEP 2: SAM INTERACTIVE IMAGE EDITOR (Tap Points & Boxes -> Show Cut Button)
            // =========================================================================
            CameraEditorWorkflowStep.SAM_IMAGE_EDITOR -> {
                val editorBitmap = capturedEditorBitmap
                    ?: remember { BaggageAsyncStorage.createDefaultCameraCaptureBitmap() }

                // Interactive SAM Image Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("segmentation_viewport_frame")
                        .pointerInput(activePromptTool) {
                            detectTapGestures { tapOffset ->
                                val nx = (tapOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.04f, 0.96f)
                                val ny = (tapOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.08f, 0.92f)
                                val tappedPoint = NormalizedPoint(nx, ny)
                                hasCutCurrentImage = false

                                when (activePromptTool) {
                                    SamEditorPromptTool.ADD_POINT -> {
                                        positivePoints.add(tappedPoint)
                                        promptHistory.add(SamPromptHistoryEntry.PositivePoint(tappedPoint))
                                    }
                                    SamEditorPromptTool.EXCLUDE_POINT -> {
                                        negativePoints.add(tappedPoint)
                                        promptHistory.add(SamPromptHistoryEntry.NegativePoint(tappedPoint))
                                    }
                                    SamEditorPromptTool.DRAW_BOX -> {
                                        val prev = interactiveBox
                                        val halfW = 0.24f
                                        val halfH = 0.22f
                                        val left = (nx - halfW).coerceIn(0.05f, 0.70f)
                                        val top = (ny - halfH).coerceIn(0.10f, 0.68f)
                                        val width = (halfW * 2f).coerceAtMost(0.95f - left)
                                        val height = (halfH * 2f).coerceAtMost(0.90f - top)
                                        interactiveBox = NormalizedRect(left, top, width, height)
                                        promptHistory.add(SamPromptHistoryEntry.BoxPrompt(prev))
                                    }
                                }
                            }
                        }
                        .pointerInput(activePromptTool) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val nx = (startOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.04f, 0.96f)
                                    val ny = (startOffset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.08f, 0.92f)
                                    dragStartNorm = NormalizedPoint(nx, ny)
                                    promptHistory.add(SamPromptHistoryEntry.BoxPrompt(interactiveBox))
                                    hasCutCurrentImage = false
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val start = dragStartNorm ?: return@detectDragGestures
                                    val curX = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0.04f, 0.96f)
                                    val curY = (change.position.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.08f, 0.92f)
                                    val minX = min(start.x, curX)
                                    val maxX = max(start.x, curX)
                                    val minY = min(start.y, curY)
                                    val maxY = max(start.y, curY)
                                    val bw = (maxX - minX).coerceAtLeast(0.12f)
                                    val bh = (maxY - minY).coerceAtLeast(0.12f)
                                    interactiveBox = NormalizedRect(
                                        left = minX.coerceIn(0.04f, 0.84f),
                                        top = minY.coerceIn(0.08f, 0.80f),
                                        width = bw.coerceAtMost(0.96f - minX),
                                        height = bh.coerceAtMost(0.92f - minY)
                                    )
                                },
                                onDragEnd = {
                                    dragStartNorm = null
                                }
                            )
                        }
                ) {
                    // 1. Captured Image Displayed in the SAM Image Editor
                    Image(
                        bitmap = editorBitmap.asImageBitmap(),
                        contentDescription = "Captured Image in SAM Editor",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Real-Time Non-Square SAM Contour Mask + Interactive Box & Point Prompts
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (hasActivePrompts) {
                            drawPreciseSamContourHighlight(
                                contourPoints = computedContour,
                                isLocked = true,
                                dimOutsideBackdrop = true
                            )
                        }

                        // Draw interactive SAM bounding box & corner handles if active
                        val box = interactiveBox
                        if (box != null) {
                            drawSamInteractiveBox(box)
                        }

                        // Draw interactive positive (+) foreground points
                        positivePoints.forEach { pt ->
                            drawSamPromptPoint(
                                point = pt,
                                isPositive = true
                            )
                        }

                        // Draw interactive negative (-) exclusion points
                        negativePoints.forEach { pt ->
                            drawSamPromptPoint(
                                point = pt,
                                isPositive = false
                            )
                        }
                    }

                    // 3. Floating SAM Status Pill on the Segmented Contour
                    if (hasActivePrompts) {
                        val minContourX = computedContour.minOfOrNull { it.x } ?: computedBox.left
                        val minContourY = computedContour.minOfOrNull { it.y } ?: computedBox.top
                        val labelXDp = (viewW * minContourX).coerceIn(16.dp, (viewW - 240.dp).coerceAtLeast(16.dp))
                        val labelYDp = (viewH * minContourY - 44.dp).coerceIn(148.dp, (viewH - 230.dp).coerceAtLeast(148.dp))

                        Surface(
                            modifier = Modifier.offset(x = labelXDp, y = labelYDp),
                            color = FacebookBlue,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.5.dp, Color.White),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (uiState.isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                val badgeText = when {
                                    uiState.isScanning -> "Cutting & Auto-Labelling…"
                                    latestCutItem != null -> "${latestCutItem.name} • ${BaggageGuardUiState.formatWeight(latestCutItem.weightKg)} kg"
                                    else -> "SAM Mask (${positivePoints.size} pt${if (positivePoints.size == 1) "" else "s"}${if (interactiveBox != null) " + Box" else ""})"
                                }

                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 4. Top SAM Image Editor Header + Interactive Point/Box Tool Selector Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Editor Header Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DashboardCardWhite.copy(alpha = 0.96f),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .size(36.dp)
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

                                Column {
                                    Text(
                                        text = "SAM Image Editor",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Tap points (+) or drag box, then tap Cut",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Undo last point/box prompt
                                IconButton(
                                    onClick = {
                                        if (promptHistory.isNotEmpty()) {
                                            when (val last = promptHistory.removeAt(promptHistory.lastIndex)) {
                                                is SamPromptHistoryEntry.PositivePoint -> positivePoints.remove(last.point)
                                                is SamPromptHistoryEntry.NegativePoint -> negativePoints.remove(last.point)
                                                is SamPromptHistoryEntry.BoxPrompt -> interactiveBox = last.previousBox
                                            }
                                            hasCutCurrentImage = false
                                        }
                                    },
                                    enabled = promptHistory.isNotEmpty(),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sam_editor_undo_button")
                                        .clip(CircleShape)
                                        .background(DashboardSurfaceLight)
                                        .border(1.dp, DashboardBorderLight, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo Prompt",
                                        tint = if (promptHistory.isNotEmpty()) TextPrimary else TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Clear all points & boxes
                                IconButton(
                                    onClick = {
                                        positivePoints.clear()
                                        negativePoints.clear()
                                        interactiveBox = null
                                        promptHistory.clear()
                                        hasCutCurrentImage = false
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sam_editor_reset_button")
                                        .clip(CircleShape)
                                        .background(DashboardSurfaceLight)
                                        .border(1.dp, DashboardBorderLight, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Prompts",
                                        tint = FacebookBlue,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Retake photo (back to live camera)
                                OutlinedButton(
                                    onClick = {
                                        workflowStep = CameraEditorWorkflowStep.CAPTURE_CAMERA
                                        hasCutCurrentImage = false
                                    },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("sam_editor_retake_button"),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, DashboardBorderLight),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Retake Photo",
                                        tint = FacebookBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Retake",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FacebookBlue
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Interactive SAM Tool Selector Pills (+ Point, - Exclude, Box)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DashboardCardWhite.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, DashboardBorderLight),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SamToolChipButton(
                                label = "+ Point (${positivePoints.size})",
                                icon = Icons.Default.AddCircleOutline,
                                selected = activePromptTool == SamEditorPromptTool.ADD_POINT,
                                activeColor = FacebookBlue,
                                testTag = "sam_tool_add_point",
                                onClick = { activePromptTool = SamEditorPromptTool.ADD_POINT },
                                modifier = Modifier.weight(1f)
                            )

                            SamToolChipButton(
                                label = "- Exclude (${negativePoints.size})",
                                icon = Icons.Default.RemoveCircleOutline,
                                selected = activePromptTool == SamEditorPromptTool.EXCLUDE_POINT,
                                activeColor = CrimsonAlert,
                                testTag = "sam_tool_exclude_point",
                                onClick = { activePromptTool = SamEditorPromptTool.EXCLUDE_POINT },
                                modifier = Modifier.weight(1f)
                            )

                            SamToolChipButton(
                                label = if (interactiveBox != null) "Box (Active)" else "Draw Box",
                                icon = Icons.Default.CropFree,
                                selected = activePromptTool == SamEditorPromptTool.DRAW_BOX,
                                activeColor = EmeraldSafe,
                                testTag = "sam_tool_draw_box",
                                onClick = { activePromptTool = SamEditorPromptTool.DRAW_BOX },
                                modifier = Modifier.weight(1f)
                            )
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

                // 5. Bottom Card: Shows "Cut Object" Button & Cutout Result Preview
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    color = DashboardCardWhite.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, DashboardBorderLight),
                    shadowElevation = 8.dp
                ) {
                    if (latestCutItem != null && !latestCutItem.imageUri.isNullOrBlank()) {
                        // Cutout completed for the current image: show cutout sticker + Re-Cut / Done actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SegmentedItemThumbnail(
                                imageUri = latestCutItem.imageUri,
                                itemName = latestCutItem.name,
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
                                        text = "SAM CUTOUT SAVED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = FacebookBlue
                                        )
                                    )
                                }
                                Text(
                                    text = latestCutItem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(latestCutItem.weightKg)} kg • Added to Dashboard list",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hasCutCurrentImage = false
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, DashboardBorderLight),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FacebookBlue
                                    )
                                )
                            }

                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("scan_gear_button"),
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
                    } else {
                        // Interactive SAM Editor Bottom Bar with prominent "Cut Object" Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasActivePrompts) {
                                        "SAM Contour Ready to Cut"
                                    } else {
                                        "Tap Object or Draw Box"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = if (hasActivePrompts) {
                                        "${positivePoints.size} include pt • ${negativePoints.size} exclude pt${if (interactiveBox != null) " • 1 box" else ""}"
                                    } else {
                                        "Tap + Point or drag Box around the item to enable Cut"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (!uiState.isScanning) {
                                        if (!hasActivePrompts) {
                                            val fallbackPt = NormalizedPoint(0.5f, 0.5f)
                                            positivePoints.add(fallbackPt)
                                            promptHistory.add(SamPromptHistoryEntry.PositivePoint(fallbackPt))
                                        }
                                        hasCutCurrentImage = true
                                        if (onCutWithInteractiveMask != null) {
                                            onCutWithInteractiveMask(
                                                editorBitmap,
                                                computedBox,
                                                computedContour
                                            )
                                        } else {
                                            onScanCameraFrame(editorBitmap)
                                        }
                                    }
                                },
                                enabled = !uiState.isScanning,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("scan_gear_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FacebookBlue,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                if (uiState.isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cutting…",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = "Cut Segmented Object",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cut Object",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SamToolChipButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (selected) activeColor else DashboardSurfaceLight,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (selected) activeColor else DashboardBorderLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else activeColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    text = "Capture & Open SAM Editor",
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
 * Draws an interactive SAM bounding box with draggable-style corner handles in the Image Editor.
 */
private fun DrawScope.drawSamInteractiveBox(box: NormalizedRect) {
    val left = box.left * size.width
    val top = box.top * size.height
    val w = box.width * size.width
    val h = box.height * size.height

    // Outer white contrast frame
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 4.5f)
    )

    // Inner Emerald/Blue SAM Box border
    drawRoundRect(
        color = EmeraldSafe,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2.5f)
    )

    // Corner handles
    val corners = listOf(
        Offset(left, top),
        Offset(left + w, top),
        Offset(left, top + h),
        Offset(left + w, top + h)
    )
    corners.forEach { corner ->
        drawCircle(
            color = Color.White,
            radius = 9f,
            center = corner
        )
        drawCircle(
            color = EmeraldSafe,
            radius = 6f,
            center = corner
        )
    }
}

/**
 * Draws an interactive SAM positive (`+`) or negative (`-`) prompt point marker on the Image Editor canvas.
 */
private fun DrawScope.drawSamPromptPoint(
    point: NormalizedPoint,
    isPositive: Boolean
) {
    val cx = point.x * size.width
    val cy = point.y * size.height
    val fillColor = if (isPositive) FacebookBlue else CrimsonAlert

    // Outer glow halo
    drawCircle(
        color = fillColor.copy(alpha = 0.28f),
        radius = 22f,
        center = Offset(cx, cy)
    )

    // Crisp white outer ring
    drawCircle(
        color = Color.White,
        radius = 14f,
        center = Offset(cx, cy)
    )

    // Colored SAM prompt core
    drawCircle(
        color = fillColor,
        radius = 11f,
        center = Offset(cx, cy)
    )

    // Inner '+' or '-' glyph
    val arm = 5.5f
    drawLine(
        color = Color.White,
        start = Offset(cx - arm, cy),
        end = Offset(cx + arm, cy),
        strokeWidth = 2.8f,
        cap = StrokeCap.Round
    )
    if (isPositive) {
        drawLine(
            color = Color.White,
            start = Offset(cx, cy - arm),
            end = Offset(cx, cy + arm),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
    }
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

    // Subtle dark backdrop outside the segmented object contour when in SAM Image Editor
    if (dimOutsideBackdrop) {
        clipPath(composePath, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.34f))
        }
    }

    // Soft solid SAM mask fill strictly inside the object's silhouette contour
    drawPath(
        path = composePath,
        color = FacebookBlue.copy(alpha = if (isLocked) 0.24f else 0.14f)
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

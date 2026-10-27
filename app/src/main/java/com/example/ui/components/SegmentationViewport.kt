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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

private const val TAG = "SegmentationViewport"

/**
 * Renders the saved SAM cutout image (decoded from AsyncStorage data URI or local file)
 * for display on the Dashboard item list, bottom sheet, and Camera cutout preview.
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
            .clip(RoundedCornerShape(10.dp))
            .background(FacebookBlueSoftBg)
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = itemName,
                contentScale = ContentScale.Crop,
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
 * Dashboard-themed Full-Screen Camera with Single-Item SAM Segmentation,
 * Label Highlight, Cutout Image Extraction, and AsyncStorage Persistence.
 * Bottom navigation bar is hidden while this screen is active.
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
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

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

    // Only ONE segmented item is highlighted and cut out
    val singleSegmentedItem: BaggageItem? = remember(uiState.lastSegmentedItem, uiState.activeItems) {
        uiState.lastSegmentedItem
            ?: uiState.activeItems.firstOrNull { it.boundingBox != null }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("fullscreen_camera_segmentation_container"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Card matching Dashboard Theme (No MUSE-SPARK-1.3 // META SAM READY badge, no Rig tabs)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DashboardCardWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
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
                            text = "SAM Item Cutout Scanner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Segment 1 item, highlight label & save cutout image",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // Camera Permission Banner (Dashboard Theme) if permission not yet granted
        if (!hasCameraPermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("camera_permission_prompt_banner"),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FacebookBlue)
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FacebookBlueSoftBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Allow camera access or pick a photo to segment and cut out an item.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FacebookBlue,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Allow",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Camera Viewport Card (Dashboard White Card Theme, No Scanning Animation, No Dashed Lines)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = DashboardCardWhite,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashboardSurfaceLight)
                    .testTag("segmentation_viewport_frame")
            ) {
                val viewW = maxWidth
                val viewH = maxHeight
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
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FacebookBlueSoftBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Center One Item in Frame",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "SAM will highlight, label, and cut out the item as an image for your Dashboard list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Single-Item Solid SAM Mask Highlight Overlay (No scanning laser, no dashed lines)
                if (singleSegmentedItem?.boundingBox != null) {
                    val box = singleSegmentedItem.boundingBox
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawSingleItemSamHighlight(
                            leftRatio = box.left,
                            topRatio = box.top,
                            widthRatio = box.width,
                            heightRatio = box.height
                        )
                    }

                    // Highlighted Item Label Pill on top of the single segmented item
                    val labelXDp = (viewW * box.left).coerceIn(8.dp, (viewW - 180.dp).coerceAtLeast(8.dp))
                    val labelYDp = (viewH * box.top - 38.dp).coerceAtLeast(8.dp)

                    Surface(
                        modifier = Modifier.offset(x = labelXDp, y = labelYDp),
                        color = FacebookBlue,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, Color.White),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = singleSegmentedItem.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
        }

        // Error Banner if any
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

        // Segmented Cutout Image Preview Card & Primary Segment Action (Dashboard Theme)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DashboardCardWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (singleSegmentedItem != null) {
                    Surface(
                        color = FacebookBlueSoftBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SegmentedItemThumbnail(
                                imageUri = singleSegmentedItem.imageUri,
                                itemName = singleSegmentedItem.name,
                                sizeDp = 56.dp
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
                                        text = "SAM Cutout Saved to AsyncStorage",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
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
                                    text = "${BaggageGuardUiState.formatWeight(singleSegmentedItem.weightKg)} kg",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateBack,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FacebookBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FacebookBlue
                                    )
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            onRequestCameraPermission()
                        }
                        val frameBitmap = customPhotoBitmap ?: previewViewRef?.bitmap
                        onScanCameraFrame(frameBitmap)
                    },
                    enabled = !uiState.isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("scan_gear_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        disabledContainerColor = FacebookBlue.copy(alpha = 0.6f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Segmenting & Cutting Out Item…",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Segment & Cut Out Item",
                            style = MaterialTheme.typography.titleMedium.copy(
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
                    val box = singleSegmentedItem.boundingBox
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawSingleItemSamHighlight(
                            leftRatio = box.left,
                            topRatio = box.top,
                            widthRatio = box.width,
                            heightRatio = box.height
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
                    text = "Segment & Cut Out Item",
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
 * Draws a clean, solid SAM segmentation highlight over ONLY the single detected item.
 * No scanning animation, no laser lines, and no dashed line shapes.
 */
private fun DrawScope.drawSingleItemSamHighlight(
    leftRatio: Float,
    topRatio: Float,
    widthRatio: Float,
    heightRatio: Float
) {
    val w = size.width
    val h = size.height
    val left = leftRatio * w
    val top = topRatio * h
    val rectW = widthRatio * w
    val rectH = heightRatio * h

    // Soft solid SAM mask highlight fill
    drawRoundRect(
        color = FacebookBlue.copy(alpha = 0.22f),
        topLeft = Offset(left, top),
        size = Size(rectW, rectH),
        cornerRadius = CornerRadius(22f, 22f)
    )

    // Outer white cutout halo
    drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(left, top),
        size = Size(rectW, rectH),
        cornerRadius = CornerRadius(22f, 22f),
        style = Stroke(width = 5f)
    )

    // Inner solid FacebookBlue SAM contour border (solid line, no dashes)
    drawRoundRect(
        color = FacebookBlue,
        topLeft = Offset(left, top),
        size = Size(rectW, rectH),
        cornerRadius = CornerRadius(22f, 22f),
        style = Stroke(width = 2.5f)
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

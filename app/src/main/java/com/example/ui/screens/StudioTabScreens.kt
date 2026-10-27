package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.AirlineCarrier
import com.example.model.AirlinePreset
import com.example.ui.components.AirlineBookingProfileCard
import com.example.model.BaggageGuardUiState
import com.example.model.GearCategory
import com.example.model.GearRigPreset
import com.example.model.ItemSource
import com.example.ui.theme.AmberManual
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DashboardBorderLight
import com.example.ui.theme.DashboardCardWhite
import com.example.ui.theme.DashboardChipGray
import com.example.ui.theme.DashboardSurfaceLight
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.FacebookBlueSoftBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QuickCameraScannerBanner(
    uiState: BaggageGuardUiState,
    onOpenCameraTab: () -> Unit,
    onTriggerQuickScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_camera_shortcut_card")
            .clickable { onOpenCameraTab() },
        color = DashboardCardWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DashboardBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FacebookBlueSoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(FacebookBlue)
                        )
                        Text(
                            text = uiState.scanPhaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = FacebookBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SAM Item Cutout Scanner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.activeItems.count { it.source == ItemSource.AI_SCANNED }} items segmented • ${BaggageGuardUiState.formatWeight(uiState.aiScannedWeightKg)} kg",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onOpenCameraTab,
                modifier = Modifier
                    .height(40.dp)
                    .testTag("open_camera_tab_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FacebookBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Open Camera",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun LuggageBreakdownTabScreen(
    uiState: BaggageGuardUiState,
    onSelectRig: (GearRigPreset) -> Unit,
    onRestoreItem: (String) -> Unit,
    onRestoreAllShifted: () -> Unit,
    onOpenCameraTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("luggage_tab_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "luggage_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Luggage & Mass Breakdown",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Text(
                    text = "Volumetric category distribution and personal item buffer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Category Mass Distribution Card
        item(key = "category_distribution_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CATEGORY MASS TELEMETRY",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(uiState.totalWeightKg)} kg Total",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                    }

                    GearCategory.entries.forEach { category ->
                        val catItems = uiState.activeItems.filter { it.category == category }
                        val catWeight = catItems.sumOf { it.weightKg }
                        val share = if (uiState.totalWeightKg > 0.0) {
                            (catWeight / uiState.totalWeightKg).toFloat().coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val animatedShare by animateFloatAsState(
                            targetValue = share,
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            label = "cat_share_${category.name}"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "(${catItems.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(catWeight)} kg (${(share * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(DashboardChipGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedShare)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(if (catWeight > 0) FacebookBlue else DashboardBorderLight)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personal Item Buffer Card
        item(key = "personal_buffer_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PERSONAL ITEM BUFFER",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Items shifted out of main luggage to stay within airline limits",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }

                        if (uiState.shiftedItems.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onRestoreAllShifted,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (uiState.shiftedItems.isEmpty()) {
                        Surface(
                            color = DashboardSurfaceLight,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DashboardBorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "No items currently shifted to personal bag. All gear is packed in main luggage.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        uiState.shiftedItems.forEach { item ->
                            Surface(
                                color = DashboardSurfaceLight,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DashboardBorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${BaggageGuardUiState.formatWeight(item.weightKg)} kg • ${item.category.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onRestoreItem(item.id) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Unpack to Main", style = MaterialTheme.typography.labelSmall)
                                    }
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
fun SettingsTabScreen(
    uiState: BaggageGuardUiState,
    onToggleBoundingBoxes: () -> Unit,
    onToggleHeatmap: () -> Unit,
    onSelectPreset: (AirlinePreset) -> Unit,
    onSelectRig: (GearRigPreset) -> Unit,
    onRunScanInCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_tab_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "settings_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Scanner & Allowance Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Text(
                    text = "Configure muse-spark-1.3 vision model, Meta SAM segmentation overlays, and default airline thresholds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // AI Vision & Camera Overlay Controls
        item(key = "vision_settings_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CAMERA & AI SEGMENTATION OVERLAYS",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }

                    // Toggle 1: Bounding Boxes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DashboardSurfaceLight)
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
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Meta SAM Bounding Boxes",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Render real-time object masks and mass callouts in camera viewport",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.showBoundingBoxes,
                            onCheckedChange = { onToggleBoundingBoxes() },
                            modifier = Modifier.testTag("settings_toggle_boxes"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FacebookBlue
                            )
                        )
                    }

                    // Toggle 2: Gravimetric Heatmap
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DashboardSurfaceLight)
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
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Gravimetric Density Heatmap",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Visualize center-of-mass hotspots inside the luggage frame",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.showHeatmapOverlay,
                            onCheckedChange = { onToggleHeatmap() },
                            modifier = Modifier.testTag("settings_toggle_heatmap"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FacebookBlue
                            )
                        )
                    }

                    Button(
                        onClick = onRunScanInCamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_launch_camera_scan"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FacebookBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Launch Camera & Run Multi-Agent Scan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Default Airline Allowance Selector
        item(key = "airline_allowance_settings_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AIRLINE WEIGHT ALLOWANCE TIER",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }

                    AirlinePreset.entries.forEach { preset ->
                        val isSelected = uiState.selectedPreset == preset
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPreset(preset) },
                            color = if (isSelected) FacebookBlueSoftBg else DashboardSurfaceLight,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FacebookBlue else DashboardBorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = preset.bagType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "${BaggageGuardUiState.formatWeight(preset.limitKg)} kg",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) FacebookBlue else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // React Native + iOS App Store Ready Structure Card
        item(key = "react_native_ios_structure_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REACT NATIVE & iOS APP STORE READY",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Surface(
                            color = FacebookBlueSoftBg,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "iOS + ANDROID",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = FacebookBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Cross-platform React Native (Expo + iOS CocoaPods) structure is configured in the project root for direct Apple App Store publishing:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = TextSecondary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DashboardSurfaceLight)
                            .border(1.dp, DashboardBorderLight, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• App.tsx & src/screens/DashboardScreen.tsx",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary
                        )
                        Text(
                            text = "• src/components/SegmentationCameraModal.tsx (expo-camera + Meta SAM)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary
                        )
                        Text(
                            text = "• src/services/museSparkMetaClient.ts (muse-spark-1.3)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary
                        )
                        Text(
                            text = "• app.json, eas.json, ios/Podfile & ios/BaggageGuard/Info.plist",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = TextPrimary
                        )
                        Text(
                            text = "• Build & Publish: eas build --platform ios && eas submit --platform ios",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FacebookBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTabScreen(
    uiState: BaggageGuardUiState,
    userLabel: String?,
    userEmail: String? = null,
    userPhotoUrl: String? = null,
    onSelectCarrier: (AirlineCarrier) -> Unit = {},
    onBookingCodeChange: (String) -> Unit = {},
    onConnectAndAnalyzeBooking: (String?, String?) -> Unit = { _, _ -> },
    onApplyAllowanceMode: (Boolean) -> Unit = {},
    onNavigateBackOrSignOut: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isGuest = userLabel == null || userLabel.contains("Guest", ignoreCase = true) || userLabel.equals("Log In", ignoreCase = true)
    val resolvedName = userLabel?.takeIf { !isGuest && it.isNotBlank() } ?: uiState.connectedBooking.passengerName
    val resolvedEmail = userEmail?.takeIf { it.isNotBlank() } ?: uiState.connectedBooking.passengerEmail
    val aiCount = uiState.activeItems.count { it.source == ItemSource.AI_SCANNED }
    val manualCount = uiState.activeItems.count { it.source == ItemSource.MANUAL_ENTRY }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_tab_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "profile_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Traveler Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Text(
                    text = "Account identity, verified airline booking, and baggage telemetry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Traveler Identity Hero Card (Simple White Card)
        item(key = "profile_identity_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FacebookBlueSoftBg)
                                .border(2.dp, FacebookBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = resolvedName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    loading = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FacebookBlue,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    },
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FacebookBlue,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = resolvedEmail,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGuest) Icons.Default.VerifiedUser else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isGuest) "LOCAL GUEST SESSION" else "FIREBASE CLOUD SYNC ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FacebookBlue
                                )
                            }
                        }
                    }

                    Surface(
                        color = DashboardSurfaceLight,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DashboardBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Loadout Preset",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "${uiState.selectedRig.title} (${uiState.selectedPreset.shortLabel})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = FacebookBlue
                            )
                        }
                    }
                }
            }
        }

        // Verified Airline Booking Details Card (Matched by Name & Email + Muse Spark 1.3)
        item(key = "profile_airline_booking_inclusion_card") {
            AirlineBookingProfileCard(
                uiState = uiState,
                passengerName = resolvedName,
                passengerEmail = resolvedEmail,
                onSelectCarrier = onSelectCarrier,
                onBookingCodeChange = onBookingCodeChange,
                onConnectAndAnalyzeBooking = onConnectAndAnalyzeBooking,
                onApplyAllowanceMode = onApplyAllowanceMode
            )
        }

        // Telemetry Metrics Grid
        item(key = "profile_metrics_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
                color = DashboardCardWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BAGGAGE TELEMETRY SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = "Total Packed Mass",
                            value = "${BaggageGuardUiState.formatWeight(uiState.totalWeightKg)} kg",
                            sublabel = "Limit: ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg",
                            accentColor = if (uiState.isOverweight) CrimsonAlert else FacebookBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "AI-Scanned Gear",
                            value = "$aiCount items",
                            sublabel = "${BaggageGuardUiState.formatWeight(uiState.aiScannedWeightKg)} kg total",
                            accentColor = ElectricIndigo,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatTile(
                            label = "Manual Overrides",
                            value = "$manualCount items",
                            sublabel = "${BaggageGuardUiState.formatWeight(uiState.manualWeightKg)} kg total",
                            accentColor = AmberManual,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "Personal Buffer",
                            value = "${uiState.shiftedItems.size} shifted",
                            sublabel = "${BaggageGuardUiState.formatWeight(uiState.shiftedWeightKg)} kg saved",
                            accentColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Sign Out / Switch Account Action
        if (onNavigateBackOrSignOut != null) {
            item(key = "profile_account_action") {
                Button(
                    onClick = onNavigateBackOrSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .height(50.dp)
                        .testTag("profile_sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGuest) "Sign In / Create Cloud Account" else "Switch Account / Sign Out",
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

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    sublabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DashboardSurfaceLight,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DashboardBorderLight)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
    }
}

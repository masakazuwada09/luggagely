package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.model.AirlinePreset
import com.example.model.BaggageGuardUiState
import com.example.model.BaggageGuardViewModel
import com.example.model.BaggageItem
import com.example.model.GearCategory
import com.example.model.ItemSource
import com.example.model.ManifestFilter
import com.example.ui.components.ConnectedAirlineDashboardBanner
import com.example.ui.components.FullScreenCameraSegmentationScreen
import com.example.ui.components.SegmentationViewportCard
import com.example.ui.components.StickyBottomNavBar
import com.example.ui.components.StudioTab
import com.example.ui.screens.LuggageBreakdownTabScreen
import com.example.ui.screens.ProfileTabScreen
import com.example.ui.screens.QuickCameraScannerBanner
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.theme.AmberManual
import com.example.ui.theme.AmberManualGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DashboardBorderLight
import com.example.ui.theme.DashboardCardWhite
import com.example.ui.theme.DashboardChipGray
import com.example.ui.theme.DashboardFeedGrayBg
import com.example.ui.theme.DashboardSurfaceLight
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.FacebookBlueSoftBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PureWhiteCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BaggageGuardStudio(
    viewModel: BaggageGuardViewModel = viewModel(),
    userLabel: String? = null,
    userEmail: String? = null,
    userPhotoUrl: String? = null,
    isGoogleSignedIn: Boolean = false,
    onNavigateBackOrSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(StudioTab.DASHBOARD) }

    LaunchedEffect(userLabel, userEmail) {
        viewModel.syncBookingWithUserIdentity(userLabel, userEmail)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val navigateToCameraWithPermission: () -> Unit = {
        val grantedNow = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasCameraPermission = grantedNow
        currentTab = StudioTab.CAMERA
        if (!grantedNow) {
            runCatching { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
        }
    }

    BackHandler(enabled = currentTab != StudioTab.DASHBOARD) {
        currentTab = StudioTab.DASHBOARD
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg),
        containerColor = DashboardFeedGrayBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            StickyBottomNavBar(
                currentTab = currentTab,
                isScanning = uiState.isScanning,
                isOverweight = uiState.isOverweight,
                onSelectTab = { selected ->
                    if (selected == StudioTab.CAMERA) {
                        navigateToCameraWithPermission()
                    } else {
                        currentTab = selected
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            val isExpandedScreen = maxWidth >= 840.dp

            Crossfade(
                targetState = currentTab,
                animationSpec = tween(220),
                label = "studio_bottom_nav_crossfade"
            ) { activeTab ->
                when (activeTab) {
                    StudioTab.DASHBOARD -> {
                        if (isExpandedScreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1.05f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    TopNavigationStatusRow(
                                        userLabel = userLabel,
                                        userPhotoUrl = userPhotoUrl,
                                        isGoogleSignedIn = isGoogleSignedIn,
                                        onOpenProfileTab = { currentTab = StudioTab.PROFILE },
                                        onNavigateBackOrSignOut = onNavigateBackOrSignOut
                                    )
                                    ConnectedAirlineDashboardBanner(
                                        uiState = uiState,
                                        onSelectCarrier = viewModel::selectAirlineCarrier,
                                        onApplyAllowanceMode = viewModel::applyBookingAllowanceToManifest,
                                        onOpenProfileBooking = { currentTab = StudioTab.PROFILE }
                                    )
                                    QuickCameraScannerBanner(
                                        uiState = uiState,
                                        onOpenCameraTab = navigateToCameraWithPermission,
                                        onTriggerQuickScan = {
                                            navigateToCameraWithPermission()
                                            viewModel.triggerMultiAgentScan()
                                        }
                                    )
                                    EscortAdvisoryBanner(
                                        uiState = uiState,
                                        onApplyAutoShift = viewModel::applyEscortRecommendedShift,
                                        onRestoreShifted = viewModel::restoreAllShiftedItems
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    DynamicManifestCard(
                                        uiState = uiState,
                                        onSelectPreset = viewModel::selectAirlinePreset,
                                        onSelectFilter = viewModel::setManifestFilter,
                                        onSelectItem = viewModel::selectItem,
                                        onAdjustWeight = viewModel::adjustItemWeight,
                                        onShiftItem = viewModel::toggleShiftToPersonal,
                                        onDeleteItem = viewModel::deleteItem
                                    )
                                    ManualOverrideFormCard(
                                        uiState = uiState,
                                        onNameChange = viewModel::updateManualNameInput,
                                        onWeightChange = viewModel::updateManualWeightInput,
                                        onCategoryChange = viewModel::updateManualCategory,
                                        onQuickPreset = viewModel::applyQuickManualPreset,
                                        onSubmit = viewModel::submitManualItem
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("baggage_guard_scroll_container"),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item(key = "top_nav_row") {
                                    TopNavigationStatusRow(
                                        userLabel = userLabel,
                                        userPhotoUrl = userPhotoUrl,
                                        isGoogleSignedIn = isGoogleSignedIn,
                                        onOpenProfileTab = { currentTab = StudioTab.PROFILE },
                                        onNavigateBackOrSignOut = onNavigateBackOrSignOut,
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }

                                item(key = "connected_airline_card") {
                                    ConnectedAirlineDashboardBanner(
                                        uiState = uiState,
                                        onSelectCarrier = viewModel::selectAirlineCarrier,
                                        onApplyAllowanceMode = viewModel::applyBookingAllowanceToManifest,
                                        onOpenProfileBooking = { currentTab = StudioTab.PROFILE },
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }

                                item(key = "camera_shortcut") {
                                    QuickCameraScannerBanner(
                                        uiState = uiState,
                                        onOpenCameraTab = navigateToCameraWithPermission,
                                        onTriggerQuickScan = {
                                            navigateToCameraWithPermission()
                                            viewModel.triggerMultiAgentScan()
                                        },
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }

                                item(key = "escort_advisory") {
                                    EscortAdvisoryBanner(
                                        uiState = uiState,
                                        onApplyAutoShift = viewModel::applyEscortRecommendedShift,
                                        onRestoreShifted = viewModel::restoreAllShiftedItems,
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }

                                item(key = "manifest_card") {
                                    DynamicManifestCard(
                                        uiState = uiState,
                                        onSelectPreset = viewModel::selectAirlinePreset,
                                        onSelectFilter = viewModel::setManifestFilter,
                                        onSelectItem = viewModel::selectItem,
                                        onAdjustWeight = viewModel::adjustItemWeight,
                                        onShiftItem = viewModel::toggleShiftToPersonal,
                                        onDeleteItem = viewModel::deleteItem,
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }

                                item(key = "manual_entry_form") {
                                    ManualOverrideFormCard(
                                        uiState = uiState,
                                        onNameChange = viewModel::updateManualNameInput,
                                        onWeightChange = viewModel::updateManualWeightInput,
                                        onCategoryChange = viewModel::updateManualCategory,
                                        onQuickPreset = viewModel::applyQuickManualPreset,
                                        onSubmit = viewModel::submitManualItem,
                                        modifier = Modifier.widthIn(max = 640.dp)
                                    )
                                }
                            }
                        }
                    }

                    StudioTab.CAMERA -> {
                        LaunchedEffect(Unit) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            hasCameraPermission = granted
                            if (!granted) {
                                runCatching { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                            }
                        }

                        FullScreenCameraSegmentationScreen(
                            uiState = uiState,
                            hasCameraPermission = hasCameraPermission,
                            onRequestCameraPermission = {
                                runCatching { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                            },
                            onScanCameraFrame = viewModel::scanCameraFrameWithAi,
                            onSelectRig = viewModel::selectGearRig,
                            onSelectItem = viewModel::selectItem,
                            onToggleBoxes = viewModel::toggleBoundingBoxes,
                            onToggleHeatmap = viewModel::toggleHeatmapOverlay,
                            onShiftItem = viewModel::toggleShiftToPersonal,
                            onDeleteItem = viewModel::deleteItem,
                            onDismissAiError = viewModel::dismissAiError,
                            onNavigateBack = { currentTab = StudioTab.DASHBOARD }
                        )
                    }

                    StudioTab.LUGGAGE -> {
                        LuggageBreakdownTabScreen(
                            uiState = uiState,
                            onSelectRig = viewModel::selectGearRig,
                            onRestoreItem = viewModel::toggleShiftToPersonal,
                            onRestoreAllShifted = viewModel::restoreAllShiftedItems,
                            onOpenCameraTab = navigateToCameraWithPermission
                        )
                    }

                    StudioTab.SETTINGS -> {
                        SettingsTabScreen(
                            uiState = uiState,
                            onToggleBoundingBoxes = viewModel::toggleBoundingBoxes,
                            onToggleHeatmap = viewModel::toggleHeatmapOverlay,
                            onSelectPreset = viewModel::selectAirlinePreset,
                            onSelectRig = viewModel::selectGearRig,
                            onRunScanInCamera = {
                                navigateToCameraWithPermission()
                                viewModel.triggerMultiAgentScan()
                            }
                        )
                    }

                    StudioTab.PROFILE -> {
                        ProfileTabScreen(
                            uiState = uiState,
                            userLabel = userLabel,
                            userEmail = userEmail,
                            userPhotoUrl = userPhotoUrl,
                            onBookingCodeChange = viewModel::updateBookingCodeInput,
                            onConnectAndAnalyzeBooking = viewModel::connectAndAnalyzeAirlineBooking,
                            onApplyAllowanceMode = viewModel::applyBookingAllowanceToManifest,
                            onNavigateBackOrSignOut = onNavigateBackOrSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNavigationStatusRow(
    userLabel: String?,
    userPhotoUrl: String?,
    isGoogleSignedIn: Boolean,
    onOpenProfileTab: () -> Unit,
    onNavigateBackOrSignOut: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val displayNameOnly = userLabel
        ?.takeIf { it.isNotBlank() && !it.equals("Guest Mode", ignoreCase = true) && !it.equals("Log In", ignoreCase = true) }
        ?: "Masakazu Wada"

    // No white background card wrapper on Dashboard — clean direct header row with larger profile pic and Name only
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Larger Google Profile Photo + Name Only (Boarding & Baggage Details text removed)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .testTag("dashboard_google_profile_photo")
                    .clickable { onOpenProfileTab() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = DashboardSurfaceLight,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isGoogleSignedIn) FacebookBlue else DashboardBorderLight
                    )
                ) {
                    if (!userPhotoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = userPhotoUrl,
                            contentDescription = displayNameOnly,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            loading = {
                                GoogleProfileFallbackAvatar(
                                    userLabel = displayNameOnly,
                                    isGoogleSignedIn = isGoogleSignedIn
                                )
                            },
                            error = {
                                GoogleProfileFallbackAvatar(
                                    userLabel = displayNameOnly,
                                    isGoogleSignedIn = isGoogleSignedIn
                                )
                            }
                        )
                    } else {
                        GoogleProfileFallbackAvatar(
                            userLabel = displayNameOnly,
                            isGoogleSignedIn = isGoogleSignedIn
                        )
                    }
                }

                if (isGoogleSignedIn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(EmeraldSafe)
                            .border(2.dp, DashboardFeedGrayBg, CircleShape)
                    )
                }
            }

            Text(
                text = displayNameOnly,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (userLabel != null && onNavigateBackOrSignOut != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .testTag("account_status_chip")
                    .clickable { onNavigateBackOrSignOut() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Switch Account or Sign Out",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun GoogleProfileFallbackAvatar(
    userLabel: String?,
    isGoogleSignedIn: Boolean
) {
    val initial = userLabel
        ?.trim()
        ?.firstOrNull { it.isLetterOrDigit() }
        ?.uppercaseChar()
        ?.toString()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isGoogleSignedIn) FacebookBlueSoftBg else DashboardChipGray),
        contentAlignment = Alignment.Center
    ) {
        if (!initial.isNullOrBlank()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = FacebookBlue
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Profile",
                tint = if (isGoogleSignedIn) FacebookBlue else TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun EscortAdvisoryBanner(
    uiState: BaggageGuardUiState,
    onApplyAutoShift: () -> Unit,
    onRestoreShifted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val advisory = uiState.escortAdvisory
    val containerBorderColor by animateColorAsState(
        targetValue = if (advisory.isOverweight) CrimsonAlert else DashboardBorderLight,
        animationSpec = tween(300),
        label = "advisory_border"
    )
    val containerBgColor by animateColorAsState(
        targetValue = if (advisory.isOverweight) Color(0xFFFEF2F2) else DashboardCardWhite,
        animationSpec = tween(300),
        label = "advisory_bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("escort_advisory_banner"),
        color = containerBgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, containerBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (advisory.isOverweight) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (advisory.isOverweight) CrimsonAlert else FacebookBlue,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = advisory.headline,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (advisory.isOverweight) CrimsonAlert else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = advisory.directive,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.testTag("escort_advisory_directive_text")
                    )
                }
            }

            if (advisory.isOverweight && advisory.recommendedShiftIds.isNotEmpty()) {
                Button(
                    onClick = onApplyAutoShift,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auto_shift_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_shift_action),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (uiState.shiftedItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DashboardSurfaceLight)
                        .border(1.dp, DashboardBorderLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERSONAL ITEM BUFFER: ${uiState.shiftedItems.size} SHIFTED (${BaggageGuardUiState.formatWeight(uiState.shiftedWeightKg)} KG)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ElectricIndigo
                        )
                        Text(
                            text = uiState.shiftedItems.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRestoreShifted,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("restore_shifted_button"),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicManifestCard(
    uiState: BaggageGuardUiState,
    onSelectPreset: (AirlinePreset) -> Unit,
    onSelectFilter: (ManifestFilter) -> Unit,
    onSelectItem: (String?) -> Unit,
    onAdjustWeight: (String, Double) -> Unit,
    onShiftItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.weightRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "weight_progress"
    )

    val barColor by animateColorAsState(
        targetValue = when {
            uiState.isOverweight -> CrimsonAlert
            uiState.isNearLimit -> AmberManual
            else -> FacebookBlue
        },
        animationSpec = tween(300),
        label = "bar_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DashboardBorderLight, RoundedCornerShape(16.dp)),
        color = DashboardCardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section Title + Telemetry Tag
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
                        text = stringResource(R.string.manifest_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                Text(
                    text = uiState.selectedPreset.bagType,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Selectable Airline Allowance Presets (Clean White / Facebook Blue Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AirlinePreset.entries.forEach { preset ->
                    val isSelected = uiState.selectedPreset == preset
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("airline_preset_${preset.id}")
                            .clickable { onSelectPreset(preset) },
                        color = if (isSelected) FacebookBlueSoftBg else DashboardSurfaceLight,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) FacebookBlue else DashboardBorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (preset.limitKg <= 10.0) Icons.Default.FlightTakeoff else Icons.Default.Luggage,
                                contentDescription = null,
                                tint = if (isSelected) FacebookBlue else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = preset.shortLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) FacebookBlue else TextSecondary
                            )
                        }
                    }
                }
            }

            // Clean White & Light Gray Cumulative Weight Summary Card (No Yellow, No Dark Background)
            Surface(
                color = DashboardSurfaceLight,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DashboardBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Nomad Express",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "GATE 21B • TERMINAL 4",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = FacebookBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.cumulative_weight_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = BaggageGuardUiState.formatWeight(uiState.totalWeightKg),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (uiState.isOverweight) CrimsonAlert else TextPrimary,
                                    modifier = Modifier.testTag("total_weight_value")
                                )
                                Text(
                                    text = "/ ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        val deltaSign = if (uiState.deltaKg > 0) "+" else ""
                        Surface(
                            color = if (uiState.isOverweight) Color(0xFFFEF2F2) else FacebookBlueSoftBg,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (uiState.isOverweight) CrimsonAlert else FacebookBlue)
                        ) {
                            Text(
                                text = "$deltaSign${BaggageGuardUiState.formatWeight(uiState.deltaKg)} kg",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.isOverweight) CrimsonAlert else FacebookBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Dynamic Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DashboardChipGray)
                            .testTag("weight_progress_bar")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            barColor.copy(alpha = 0.85f),
                                            barColor
                                        )
                                    )
                                )
                        )
                    }

                    // Clean Soft Blue / White Summary Strip
                    Surface(
                        color = if (uiState.isOverweight) CrimsonAlert else DashboardCardWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (uiState.isOverweight) CrimsonAlert else DashboardBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI: ${BaggageGuardUiState.formatWeight(uiState.aiScannedWeightKg)} kg • Manual: ${BaggageGuardUiState.formatWeight(uiState.manualWeightKg)} kg",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (uiState.isOverweight) Color.White else TextSecondary
                            )
                            Text(
                                text = "${(uiState.weightRatio * 100).toInt()}% LOAD",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.isOverweight) Color.White else FacebookBlue
                            )
                        }
                    }
                }
            }

            // Filter Pills (All / AI-Scanned / Manual Override)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManifestFilter.entries.forEach { filter ->
                    val isSelected = uiState.activeFilter == filter
                    Surface(
                        modifier = Modifier
                            .testTag("manifest_filter_${filter.name.lowercase()}")
                            .clickable { onSelectFilter(filter) },
                        color = if (isSelected) FacebookBlueSoftBg else DashboardSurfaceLight,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) FacebookBlue else DashboardBorderLight
                        )
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) FacebookBlue else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Interactive Manifest Item List
            val displayedItems = uiState.filteredActiveItems
            if (displayedItems.isEmpty()) {
                Surface(
                    color = DashboardSurfaceLight,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DashboardBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_manifest_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_manifest_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("manifest_items_list")
                ) {
                    displayedItems.forEach { item ->
                        ManifestItemRow(
                            item = item,
                            isSelected = uiState.selectedItemId == item.id,
                            isRecommendedForShift = item.id in uiState.escortAdvisory.recommendedShiftIds,
                            onSelect = { onSelectItem(item.id) },
                            onAdjustWeight = { delta -> onAdjustWeight(item.id, delta) },
                            onShiftToPersonal = { onShiftItem(item.id) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManifestItemRow(
    item: BaggageItem,
    isSelected: Boolean,
    isRecommendedForShift: Boolean,
    onSelect: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onShiftToPersonal: () -> Unit,
    onDelete: () -> Unit
) {
    val isAi = item.source == ItemSource.AI_SCANNED
    val indicatorColor = if (isAi) ElectricIndigo else AmberManual

    val borderColor = when {
        isSelected -> FacebookBlue
        isRecommendedForShift -> CrimsonAlert.copy(alpha = 0.75f)
        else -> DashboardBorderLight
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manifest_item_${item.id}")
            .clickable { onSelect() },
        color = if (isSelected) FacebookBlueSoftBg else DashboardSurfaceLight,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAi) {
                                    "${item.maskId ?: "META-SAM"} • MUSE-SPARK-1.3 • ${((item.confidence ?: 0.98f) * 100).toInt()}% CONF"
                                } else {
                                    "MANUAL OVERRIDE"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = indicatorColor
                            )
                            Text(
                                text = "• ${item.category.tagCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextMuted
                            )
                            if (isRecommendedForShift) {
                                Text(
                                    text = "• SHIFT TARGET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CrimsonAlert
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = DashboardCardWhite,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, DashboardBorderLight)
                    ) {
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(item.weightKg)} kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onShiftToPersonal,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("shift_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoveDown,
                            contentDescription = stringResource(R.string.shift_to_personal_cd),
                            tint = if (isRecommendedForShift) CrimsonAlert else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete_item_cd),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DashboardCardWhite)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALIBRATE MASS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustWeight(-0.10) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, DashboardBorderLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease weight by 0.1kg",
                                tint = TextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "0.1kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { onAdjustWeight(0.10) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, DashboardBorderLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase weight by 0.1kg",
                                tint = TextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "0.1kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualOverrideFormCard(
    uiState: BaggageGuardUiState,
    onNameChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onCategoryChange: (GearCategory) -> Unit,
    onQuickPreset: (String, Double, GearCategory) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue)
                )
                Column {
                    Text(
                        text = stringResource(R.string.manual_entry_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.manual_entry_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            // Quick Tap Nomad Override Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickPresets = listOf(
                    Triple("Packed Hiking Boots", 1.45, GearCategory.APPAREL),
                    Triple("TSA Liquids Bag", 0.75, GearCategory.LIQUIDS),
                    Triple("Down Winter Parka", 1.20, GearCategory.APPAREL),
                    Triple("Universal Cable Brick", 0.55, GearCategory.POWER)
                )
                quickPresets.forEachIndexed { idx, (label, kg, cat) ->
                    Surface(
                        modifier = Modifier
                            .testTag("quick_manual_preset_$idx")
                            .clickable { onQuickPreset(label, kg, cat) },
                        color = DashboardSurfaceLight,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, DashboardBorderLight)
                    ) {
                        Text(
                            text = "+ $label (${BaggageGuardUiState.formatWeight(kg)}kg)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Category Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GearCategory.entries.forEach { category ->
                    val selected = uiState.manualCategory == category
                    Surface(
                        modifier = Modifier.clickable { onCategoryChange(category) },
                        color = if (selected) FacebookBlueSoftBg else DashboardSurfaceLight,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (selected) FacebookBlue else DashboardBorderLight
                        )
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) FacebookBlue else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Inline Text Inputs for Item Name and Weight (kg)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.manualNameInput,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("manual_item_name_input"),
                    placeholder = {
                        Text(
                            text = "Item (e.g., Packed Boots)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FacebookBlue,
                        unfocusedBorderColor = DashboardBorderLight,
                        focusedContainerColor = DashboardSurfaceLight,
                        unfocusedContainerColor = DashboardSurfaceLight,
                        cursorColor = FacebookBlue
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = uiState.manualWeightInput,
                    onValueChange = onWeightChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_item_weight_input"),
                    placeholder = {
                        Text(
                            text = "kg (1.2)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    suffix = {
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FacebookBlue,
                        unfocusedBorderColor = DashboardBorderLight,
                        focusedContainerColor = DashboardSurfaceLight,
                        unfocusedContainerColor = DashboardSurfaceLight,
                        cursorColor = FacebookBlue
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        }
                    )
                )
            }

            if (uiState.manualInputError != null) {
                Text(
                    text = uiState.manualInputError,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAlert,
                    modifier = Modifier.testTag("manual_input_error_text")
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_manual_item_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FacebookBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_to_manifest),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.BaggageAsyncStorage
import com.example.model.AirlinePreset
import com.example.model.BaggageGuardUiState
import com.example.model.BaggageGuardViewModel
import com.example.model.BaggageItem
import com.example.model.GearCategory
import com.example.model.GearRigPreset
import com.example.model.ItemSource
import com.example.model.ManifestFilter
import com.example.ui.components.ConnectedAirlineDashboardBanner
import com.example.ui.components.FullScreenCameraSegmentationScreen
import com.example.ui.components.SegmentationViewportCard
import com.example.ui.components.SegmentedItemThumbnail
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showSignOutConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var bottomSheetItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var bottomSheetBagRigId by rememberSaveable { mutableStateOf<String?>(null) }

    val requestSignOutWithConfirmation: (() -> Unit)? = if (onNavigateBackOrSignOut != null) {
        { showSignOutConfirmDialog = true }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        BaggageAsyncStorage.init(context)
        viewModel.loadFromAsyncStorageIfNeeded()
    }

    LaunchedEffect(userLabel, userEmail) {
        viewModel.syncBookingWithUserIdentity(userLabel, userEmail)
    }

    val activeBottomSheetItem = uiState.items.find { it.id == bottomSheetItemId }
    if (activeBottomSheetItem != null) {
        val itemSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { bottomSheetItemId = null },
            sheetState = itemSheetState,
            containerColor = DashboardCardWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.testTag("luggage_item_details_bottom_sheet")
        ) {
            LuggageItemDetailsSheetContent(
                item = activeBottomSheetItem,
                uiState = uiState,
                onAdjustWeight = { delta -> viewModel.adjustItemWeight(activeBottomSheetItem.id, delta) },
                onToggleShift = { viewModel.toggleShiftToPersonal(activeBottomSheetItem.id) },
                onDelete = {
                    viewModel.deleteItem(activeBottomSheetItem.id)
                    bottomSheetItemId = null
                },
                onClose = { bottomSheetItemId = null }
            )
        }
    }

    val activeBottomSheetRig = GearRigPreset.entries.find { it.id == bottomSheetBagRigId }
    if (activeBottomSheetRig != null && activeBottomSheetItem == null) {
        val bagSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { bottomSheetBagRigId = null },
            sheetState = bagSheetState,
            containerColor = DashboardCardWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.testTag("luggage_bag_details_bottom_sheet")
        ) {
            LuggageBagDetailsSheetContent(
                rig = activeBottomSheetRig,
                uiState = uiState,
                onInspectItem = { itemId ->
                    bottomSheetBagRigId = null
                    viewModel.selectItem(itemId)
                    bottomSheetItemId = itemId
                },
                onClose = { bottomSheetBagRigId = null }
            )
        }
    }

    if (showSignOutConfirmDialog && onNavigateBackOrSignOut != null) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            containerColor = DashboardCardWhite,
            title = {
                Text(
                    text = "Confirm Sign Out",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your Luggagely account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onNavigateBackOrSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_sign_out_button")
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary
                    )
                }
            }
        )
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
        contentWindowInsets = if (currentTab == StudioTab.CAMERA) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.safeDrawing
        },
        bottomBar = {
            if (currentTab != StudioTab.CAMERA) {
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
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentTab == StudioTab.CAMERA) {
                        Modifier
                    } else {
                        Modifier.padding(innerPadding)
                    }
                )
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
                                        onNavigateBackOrSignOut = requestSignOutWithConfirmation
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
                                        onSelectRig = { rig ->
                                            viewModel.selectGearRig(rig)
                                            bottomSheetBagRigId = rig.id
                                        },
                                        onSelectFilter = viewModel::setManifestFilter,
                                        onSelectItem = { itemId ->
                                            viewModel.selectItem(itemId)
                                            if (itemId != null) {
                                                bottomSheetItemId = itemId
                                            }
                                        },
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
                                        onNavigateBackOrSignOut = requestSignOutWithConfirmation,
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
                                        onSelectRig = { rig ->
                                            viewModel.selectGearRig(rig)
                                            bottomSheetBagRigId = rig.id
                                        },
                                        onSelectFilter = viewModel::setManifestFilter,
                                        onSelectItem = { itemId ->
                                            viewModel.selectItem(itemId)
                                            if (itemId != null) {
                                                bottomSheetItemId = itemId
                                            }
                                        },
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
                            onSelectCarrier = viewModel::selectAirlineCarrier,
                            onBookingCodeChange = viewModel::updateBookingCodeInput,
                            onConnectAndAnalyzeBooking = viewModel::connectAndAnalyzeAirlineBooking,
                            onApplyAllowanceMode = viewModel::applyBookingAllowanceToManifest,
                            onNavigateBackOrSignOut = requestSignOutWithConfirmation
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
        ?.takeIf { it.isNotBlank() }
        ?: "Guest Traveler"

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
    onSelectRig: (GearRigPreset) -> Unit,
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

            // Clean White & Light Gray Cumulative Weight Summary Card linked to Connected Airline Booking
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
                            text = "${uiState.connectedBooking.carrier.carrierName} (${uiState.connectedBooking.flightNumber}) • PNR ${uiState.connectedBooking.bookingCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SEAT ${uiState.connectedBooking.seatNumber.substringBefore(" ")} • ${uiState.connectedBooking.carrier.iataCode}",
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

            // Dashboard Luggage Summary Row (Click to view Luggage Details in Bottom Sheet)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectRig(uiState.selectedRig) }
                    .testTag("dashboard_luggage_bag_${uiState.selectedRig.id}"),
                color = FacebookBlueSoftBg,
                shape = RoundedCornerShape(12.dp),
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Luggage,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Connected Luggage (${uiState.connectedBooking.carrier.carrierName})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "${uiState.activeItems.size} items • ${BaggageGuardUiState.formatWeight(uiState.totalWeightKg)} / ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "Luggage Details →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                    )
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

            // Interactive Manifest Item List (Click any item to open Bottom Sheet with full Luggage Details)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SegmentedItemThumbnail(
                    imageUri = item.imageUri,
                    itemName = item.name,
                    sizeDp = 44.dp
                )

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

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

@Composable
private fun LuggageItemDetailsSheetContent(
    item: BaggageItem,
    uiState: BaggageGuardUiState,
    onAdjustWeight: (Double) -> Unit,
    onToggleShift: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val isAi = item.source == ItemSource.AI_SCANNED
    val booking = uiState.connectedBooking
    val shareOfAllowance = if (uiState.limitKg > 0.0) {
        ((item.weightKg / uiState.limitKg) * 100.0).toInt().coerceAtLeast(1)
    } else {
        0
    }
    val categoryRuleAdvisory = when (item.category) {
        GearCategory.POWER ->
            "Lithium Battery Rule (${booking.carrier.carrierName}): Must be packed in your ${BaggageGuardUiState.formatWeight(booking.cabinInclusionKg)} kg Cabin Carry-On or Under-Seat Personal Bag. Prohibited in Checked Hold."
        GearCategory.COMPUTE ->
            "High-Value Electronics (${booking.carrier.carrierName}): Recommended in Cabin Carry-On or Personal Laptop Sleeve under e-Ticket ${booking.eTicketNumber}."
        GearCategory.LIQUIDS ->
            "LAGs Rule (${booking.carrier.carrierName}): Containers ≤100ml in 1L resealable pouch for Cabin; larger containers covered by ${BaggageGuardUiState.formatWeight(booking.checkedInclusionKg)} kg Checked Baggage."
        GearCategory.APPAREL ->
            "Apparel & Footwear (${booking.carrier.carrierName}): Ideal candidate for ${BaggageGuardUiState.formatWeight(booking.checkedInclusionKg)} kg Checked Baggage if Cabin exceeds ${BaggageGuardUiState.formatWeight(booking.cabinInclusionKg)} kg."
        GearCategory.ACCESSORY ->
            "General Travel Gear (${booking.carrier.carrierName}): Cleared for both ${BaggageGuardUiState.formatWeight(booking.cabinInclusionKg)} kg Cabin and ${BaggageGuardUiState.formatWeight(booking.checkedInclusionKg)} kg Checked Hold."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sheet Header: Luggage Item Name + Weight Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                SegmentedItemThumbnail(
                    imageUri = item.imageUri,
                    itemName = item.name,
                    sizeDp = 52.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LUGGAGE ITEM DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "${item.category.label} (${item.category.tagCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = FacebookBlueSoftBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FacebookBlue)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${BaggageGuardUiState.formatWeight(item.weightKg)} kg",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = FacebookBlue
                        )
                    )
                    Text(
                        text = "${(item.weightKg * 1000).toInt()} g",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }

        // Luggage Telemetry Grid Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DashboardSurfaceLight,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DETECTION SOURCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = if (isAi) "Muse Spark 1.3 + Meta SAM 3.1" else "Manual Override Entry",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SEGMENTATION & CONFIDENCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = if (isAi) {
                                "${item.maskId ?: "SAM-MASK"} • ${((item.confidence ?: 0.98f) * 100).toInt()}%"
                            } else {
                                "USER VERIFIED"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isAi) ElectricIndigo else AmberManual
                            )
                        )
                    }
                }

                HorizontalDivider(color = DashboardBorderLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT BAG PLACEMENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = if (item.isShiftedToPersonal) {
                                "Personal Item Buffer (Under-Seat Bag)"
                            } else {
                                "Main Luggage (${uiState.selectedRig.title})"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (item.isShiftedToPersonal) ElectricIndigo else TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ALLOWANCE SHARE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = "$shareOfAllowance% of ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = FacebookBlue
                            )
                        )
                    }
                }
            }
        }

        // Linked Airline Booking Context Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DashboardSurfaceLight,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LINKED AIRLINE BOOKING (${booking.carrier.carrierName.uppercase()})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                    )
                    Text(
                        text = "PNR: ${booking.bookingCode}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Text(
                    text = "${booking.passengerName} (${booking.iataPassengerName}) • E-Ticket ${booking.eTicketNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "${booking.flightNumber} • ${booking.route} • Seat ${booking.seatNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryRuleAdvisory,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = TextPrimary
                    )
                )
            }
        }

        // Weight Calibration Row inside Bottom Sheet
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = "ADJUST WEIGHT:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAdjustWeight(-0.50) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("-0.5kg", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onAdjustWeight(-0.10) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("-0.1kg", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onAdjustWeight(0.10) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+0.1kg", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onAdjustWeight(0.50) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+0.5kg", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Action Buttons Row (Shift to Personal / Delete / Close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onToggleShift,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("bottom_sheet_shift_item_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FacebookBlue)
            ) {
                Icon(
                    imageVector = if (item.isShiftedToPersonal) Icons.Default.Restore else Icons.Default.MoveDown,
                    contentDescription = null,
                    tint = FacebookBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (item.isShiftedToPersonal) "Move to Main Bag" else "Shift to Personal",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                )
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .height(46.dp)
                    .testTag("bottom_sheet_delete_item_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CrimsonAlert)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = CrimsonAlert,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAlert
                    )
                )
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("bottom_sheet_close_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FacebookBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LuggageBagDetailsSheetContent(
    rig: GearRigPreset,
    uiState: BaggageGuardUiState,
    onInspectItem: (String) -> Unit,
    onClose: () -> Unit
) {
    val booking = uiState.connectedBooking
    val itemsInBag = uiState.activeItems

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FacebookBlueSoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Luggage,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LUGGAGE BAG DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                    )
                    Text(
                        text = rig.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "${rig.subtitle} • ${rig.defaultLimit.shortLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = FacebookBlueSoftBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FacebookBlue)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${BaggageGuardUiState.formatWeight(uiState.totalWeightKg)} kg",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = FacebookBlue
                        )
                    )
                    Text(
                        text = "/ ${BaggageGuardUiState.formatWeight(uiState.limitKg)} kg",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }

        // Airline Booking & Bag Compliance Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DashboardSurfaceLight,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DashboardBorderLight)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${booking.carrier.carrierName} (${booking.flightNumber}) • PNR ${booking.bookingCode}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                )
                Text(
                    text = "Passenger: ${booking.passengerName} (${booking.iataPassengerName}) • E-Ticket ${booking.eTicketNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "${booking.route} • ${booking.terminalGate} • Seat ${booking.seatNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Text(
            text = "ITEMS IN THIS LUGGAGE (${itemsInBag.size}) • TAP ITEM FOR DETAILS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsInBag.forEach { bagItem ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onInspectItem(bagItem.id) },
                    color = DashboardSurfaceLight,
                    shape = RoundedCornerShape(12.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SegmentedItemThumbnail(
                                imageUri = bagItem.imageUri,
                                itemName = bagItem.name,
                                sizeDp = 40.dp
                            )
                            Text(
                                text = bagItem.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "${BaggageGuardUiState.formatWeight(bagItem.weightKg)} kg",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = FacebookBlue
                            )
                        )
                    }
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FacebookBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}


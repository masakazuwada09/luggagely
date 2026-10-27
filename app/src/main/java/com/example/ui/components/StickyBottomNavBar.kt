package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ContrastDarkCard
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.LightBorderSubtle
import com.example.ui.theme.LimeChartreuse
import com.example.ui.theme.LimeChartreuseDark
import com.example.ui.theme.PureWhiteCard
import com.example.ui.theme.SoftLimeTintBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class StudioTab(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenterCamera: Boolean = false
) {
    DASHBOARD(
        id = "dashboard",
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    LUGGAGE(
        id = "luggage",
        label = "Luggage",
        selectedIcon = Icons.Filled.Luggage,
        unselectedIcon = Icons.Outlined.Luggage
    ),
    CAMERA(
        id = "camera",
        label = "Camera",
        selectedIcon = Icons.Filled.PhotoCamera,
        unselectedIcon = Icons.Filled.PhotoCamera,
        isCenterCamera = true
    ),
    SETTINGS(
        id = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    ),
    PROFILE(
        id = "profile",
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@Composable
fun StickyBottomNavBar(
    currentTab: StudioTab,
    isScanning: Boolean,
    isOverweight: Boolean,
    onSelectTab: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "camera_nav_pulse")
    val scanPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .testTag("sticky_bottom_nav_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Bottom Navigation Surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureWhiteCard,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, LightBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioTab.entries.forEach { tab ->
                    if (tab.isCenterCamera) {
                        // Center slot placeholder spacing for the elevated sticky camera button
                        CenterCameraNavButton(
                            isSelected = currentTab == StudioTab.CAMERA,
                            isScanning = isScanning,
                            pulseScale = if (isScanning) scanPulse else 1f,
                            onClick = { onSelectTab(StudioTab.CAMERA) },
                            modifier = Modifier.weight(1.15f)
                        )
                    } else {
                        StandardNavTabItem(
                            tab = tab,
                            isSelected = currentTab == tab,
                            showAlertDot = tab == StudioTab.LUGGAGE && isOverweight,
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardNavTabItem(
    tab: StudioTab,
    isSelected: Boolean,
    showAlertDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else TextMuted,
        animationSpec = tween(220),
        label = "nav_icon_tint_${tab.id}"
    )
    val pillBg by animateColorAsState(
        targetValue = if (isSelected) SoftLimeTintBg else Color.Transparent,
        animationSpec = tween(220),
        label = "nav_pill_bg_${tab.id}"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag("nav_tab_${tab.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillBg)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, LimeChartreuseDark.copy(alpha = 0.6f), RoundedCornerShape(50))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            if (showAlertDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun CenterCameraNavButton(
    isSelected: Boolean,
    isScanning: Boolean,
    pulseScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(220),
        label = "center_camera_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) LimeChartreuse else ContrastDarkCard,
        animationSpec = tween(250),
        label = "center_camera_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) TextOnLime else LimeChartreuse,
        animationSpec = tween(250),
        label = "center_camera_icon"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("nav_tab_camera"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .scale(buttonScale * pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Outer halo ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = if (isSelected) 12.dp else 6.dp,
                        shape = CircleShape,
                        ambientColor = LimeChartreuseDark,
                        spotColor = LimeChartreuseDark
                    )
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(
                        width = if (isSelected) 2.5.dp else 2.dp,
                        color = if (isSelected) ContrastDarkCard else LimeChartreuse,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "AI Camera Scanner",
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Live AI status badge dot on top-right of the camera orb
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(PureWhiteCard)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (isScanning) LimeChartreuseDark else EmeraldSafe)
                )
            }
        }

        Text(
            text = "Camera",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.offset(y = (-6).dp)
        )
    }
}

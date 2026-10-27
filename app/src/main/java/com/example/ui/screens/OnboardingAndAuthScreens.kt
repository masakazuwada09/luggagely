package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DashboardBorderLight
import com.example.ui.theme.DashboardCardWhite
import com.example.ui.theme.DashboardChipGray
import com.example.ui.theme.DashboardFeedGrayBg
import com.example.ui.theme.DashboardSurfaceLight
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.FacebookBlueDark
import com.example.ui.theme.FacebookBlueSoftBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 1: Introduction / Onboarding Screen styled with the exact same clean
 * light Facebook-inspired palette as the Dashboard (`DashboardFeedGrayBg`, `DashboardCardWhite`,
 * `DashboardSurfaceLight`, `DashboardBorderLight`, `FacebookBlue`, `TextPrimary`, `TextSecondary`).
 */
@Composable
fun IntroWelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float_suitcase")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Subtle Clean Light Blue & Silver-Gray 3D Suitcase Illustration Canvas matching Dashboard palette
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatOffset.dp)
        ) {
            val w = size.width
            val h = size.height

            // Secondary Soft Slate-Blue Suitcase at bottom-left
            rotate(degrees = -12f, pivot = Offset(w * 0.18f, h * 0.82f)) {
                val secondaryLeft = -w * 0.06f
                val secondaryTop = h * 0.68f
                val secondaryW = w * 0.44f
                val secondaryH = h * 0.28f

                // Telescopic handle
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(secondaryLeft + secondaryW * 0.30f, secondaryTop - h * 0.07f),
                    size = Size(8f, h * 0.08f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(secondaryLeft + secondaryW * 0.65f, secondaryTop - h * 0.07f),
                    size = Size(8f, h * 0.08f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(secondaryLeft + secondaryW * 0.26f, secondaryTop - h * 0.08f),
                    size = Size(secondaryW * 0.46f, 14f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Soft Blue-Slate body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE7F3FF),
                            Color(0xFFD0E7FF),
                            Color(0xFFB6D8FF)
                        ),
                        start = Offset(secondaryLeft, secondaryTop),
                        end = Offset(secondaryLeft + secondaryW, secondaryTop + secondaryH)
                    ),
                    topLeft = Offset(secondaryLeft, secondaryTop),
                    size = Size(secondaryW, secondaryH),
                    cornerRadius = CornerRadius(44f, 44f)
                )
            }

            // Primary Hero Facebook-Blue & Clean Silver Ribbed Hardshell Suitcase tilted on right
            rotate(degrees = 9f, pivot = Offset(w * 0.68f, h * 0.56f)) {
                val suitW = w * 0.56f
                val suitH = h * 0.34f
                val suitLeft = w * 0.42f
                val suitTop = h * 0.38f

                // Silver Telescopic Handle Bars
                val handleLeft = suitLeft + suitW * 0.32f
                val handleRight = suitLeft + suitW * 0.68f
                val handleTop = suitTop - h * 0.095f
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(handleLeft, handleTop),
                    size = Size(12f, h * 0.11f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(handleRight, handleTop),
                    size = Size(12f, h * 0.11f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(handleLeft - 8f, handleTop - 10f),
                    size = Size((handleRight - handleLeft) + 28f, 20f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // 3D Depth Back Shell
                drawRoundRect(
                    color = FacebookBlueDark.copy(alpha = 0.25f),
                    topLeft = Offset(suitLeft + 16f, suitTop + 14f),
                    size = Size(suitW, suitH),
                    cornerRadius = CornerRadius(52f, 52f)
                )

                // Main Clean Blue Hardshell Body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            FacebookBlue,
                            FacebookBlueDark
                        ),
                        start = Offset(suitLeft, suitTop),
                        end = Offset(suitLeft + suitW, suitTop + suitH)
                    ),
                    topLeft = Offset(suitLeft, suitTop),
                    size = Size(suitW, suitH),
                    cornerRadius = CornerRadius(52f, 52f)
                )

                // Concentric Molded Ribs
                for (ring in 1..3) {
                    val inset = ring * 24f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        topLeft = Offset(suitLeft + inset, suitTop + inset),
                        size = Size(suitW - inset * 2, suitH - inset * 2),
                        cornerRadius = CornerRadius(38f - ring * 6f, 38f - ring * 6f),
                        style = Stroke(width = 4f)
                    )
                }

                // Security Luggage Strap with White ID Tag
                val strapY = suitTop + suitH * 0.40f
                val strapH = suitH * 0.13f
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(suitLeft - 4f, strapY),
                    size = Size(suitW + 14f, strapH)
                )
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(suitLeft + suitW * 0.28f, strapY - 4f),
                    size = Size(suitW * 0.22f, strapH + 8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(suitLeft + suitW * 0.56f, strapY + 6f),
                    size = Size(suitW * 0.20f, strapH - 12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Four Dual-Caster Spinner Wheels
                val wheelPositions = listOf(
                    Offset(suitLeft + suitW * 0.12f, suitTop + suitH + 16f),
                    Offset(suitLeft + suitW * 0.34f, suitTop + suitH + 26f),
                    Offset(suitLeft + suitW * 0.68f, suitTop + suitH + 18f),
                    Offset(suitLeft + suitW * 0.88f, suitTop + suitH + 30f)
                )
                wheelPositions.forEach { pos ->
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = 22f,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = 11f,
                        center = Offset(pos.x - 3f, pos.y - 2f)
                    )
                }
            }
        }

        // Foreground Typography & Bottom "Get Started" Button (no white card background or border)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Luggagely transparent mark (bigger) beside smaller Luggagely text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("intro_brand_row")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_luggagely_mark),
                        contentDescription = "Luggagely Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(68.dp)
                            .testTag("intro_luggagely_logo")
                    )
                    Text(
                        text = "Luggagely",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        modifier = Modifier.testTag("intro_headline")
                    )
                }

                Text(
                    text = "Track your belongings with ease,\nanytime, anywhere.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }

                // Compact single-line attribution below Get Started button: smaller Meta Logo + "Brought to you by MUSE SPARK 1.3"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("intro_meta_attribution")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_meta_logo),
                        contentDescription = "Meta Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(14.dp)
                            .testTag("intro_meta_logo")
                    )
                    Text(
                        text = "Brought to you by MUSE SPARK 1.3",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        maxLines = 1,
                        color = FacebookBlue
                    )
                }
            }
        }
    }
}

/**
 * Screen 2: Login Screen — borderless layout with centered large Luggagely logo,
 * travel/luggage items circling around it at 35% opacity, and only the
 * "Cloud Sync &" text below the logo, followed by Google Sign in and Facebook Sign in buttons.
 */
@Composable
fun AuthLoginSignUpScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onBackToIntro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "orbit_items")
    val orbitAngleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val circlingItems = remember {
        listOf(
            Icons.Default.Luggage,
            Icons.Default.FlightTakeoff,
            Icons.Default.AirplaneTicket,
            Icons.Default.PhotoCamera,
            Icons.Default.Scale,
            Icons.Default.Radar
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFeedGrayBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Borderless Back Button only
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToIntro,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DashboardCardWhite)
                        .testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Introduction",
                        tint = TextPrimary
                    )
                }
            }

            // Center Hero: Large Centered Luggagely Logo + Items Circling at 35% Opacity + Cloud Sync & Text Only
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .testTag("auth_logo_orbit_box"),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle Orbit Track at 35% opacity
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.35f)
                    ) {
                        val radiusPx = size.minDimension * 0.40f
                        drawCircle(
                            color = FacebookBlue,
                            radius = radiusPx,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                        )
                        drawCircle(
                            color = FacebookBlue.copy(alpha = 0.14f),
                            radius = size.minDimension * 0.25f
                        )
                    }

                    // Circling Luggage & Travel Items at 35% Opacity
                    val orbitRadiusDp = 104f
                    circlingItems.forEachIndexed { index, iconVector ->
                        val itemAngleDeg = orbitAngleDeg + (index * (360f / circlingItems.size))
                        val itemAngleRad = Math.toRadians(itemAngleDeg.toDouble())
                        val offsetX = (cos(itemAngleRad) * orbitRadiusDp).dp
                        val offsetY = (sin(itemAngleRad) * orbitRadiusDp).dp

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .alpha(0.35f)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DashboardCardWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Large Centered Luggagely Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_luggagely_mark),
                        contentDescription = "Luggagely Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(124.dp)
                            .testTag("auth_centered_logo")
                    )
                }

                // Cloud Sync & Text Below Logo Only (No borders, no other texts)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cloud Sync & Real-Time SAM 3.1 Manifest",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("auth_cloud_sync_title")
                    )
                    Text(
                        text = "Your scanned gear weights, live airline PNR bookings, and personal bag shifts stay synced securely with Google Sign-In.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                AnimatedVisibility(visible = authErrorMessage != null) {
                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = authErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Borderless Social Sign-In Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. First Button: "Google Sign in" with Google Logo (No border)
                Button(
                    onClick = {
                        isLoading = true
                        authErrorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                authErrorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardCardWhite,
                        contentColor = TextPrimary,
                        disabledContainerColor = DashboardCardWhite.copy(alpha = 0.7f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = FacebookBlue,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_social_google),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google Sign in",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }

                // 2. Second Button Below: "Facebook Sign in" with Facebook Logo (No border)
                Button(
                    onClick = {
                        isLoading = true
                        authErrorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                authErrorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("facebook_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        disabledContainerColor = FacebookBlue.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_social_facebook),
                            contentDescription = "Facebook Logo",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Facebook Sign in",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }

                // 3. Guest Option Button: "Continue As Guest" (No border)
                Button(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("continue_as_guest_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DashboardSurfaceLight,
                        contentColor = TextSecondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue As Guest",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

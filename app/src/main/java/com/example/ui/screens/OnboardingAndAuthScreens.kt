package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.ContrastDarkCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.LightBorderSubtle
import com.example.ui.theme.LightCreamBg
import com.example.ui.theme.LimeChartreuse
import com.example.ui.theme.LimeChartreuseDark
import com.example.ui.theme.PureWhiteCard
import com.example.ui.theme.SoftLimeTintBg
import com.example.ui.theme.SuitcaseAmber
import com.example.ui.theme.SuitcaseCoral
import com.example.ui.theme.SuitcaseYellow
import com.example.ui.theme.TextOnLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 1: Introduction / Onboarding Screen matching the left panel of the user's reference image:
 * "Welcome to our Luggage Tracker App! Track your belongings with ease, anytime, anywhere."
 * Features floating 3D-styled yellow & coral hardshell spinner suitcases and the bright lime "Get Started" pill button.
 */
@Composable
fun IntroWelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float_suitcase")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F8F3),
                        Color(0xFFF3F6E4),
                        Color(0xFFE7F2A7)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Background Floating Yellow & Coral 3D Suitcase Illustration Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatOffset.dp)
        ) {
            val w = size.width
            val h = size.height

            // Secondary Coral-Orange Suitcase at bottom-left
            rotate(degrees = -12f, pivot = Offset(w * 0.18f, h * 0.82f)) {
                val coralLeft = -w * 0.06f
                val coralTop = h * 0.68f
                val coralW = w * 0.44f
                val coralH = h * 0.28f

                // Telescopic handle
                drawRoundRect(
                    color = Color(0xFFD1D5DB),
                    topLeft = Offset(coralLeft + coralW * 0.30f, coralTop - h * 0.07f),
                    size = Size(8f, h * 0.08f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFFD1D5DB),
                    topLeft = Offset(coralLeft + coralW * 0.65f, coralTop - h * 0.07f),
                    size = Size(8f, h * 0.08f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF9CA3AF),
                    topLeft = Offset(coralLeft + coralW * 0.26f, coralTop - h * 0.08f),
                    size = Size(coralW * 0.46f, 14f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Coral body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFB923C), SuitcaseCoral, Color(0xFFEA580C)),
                        start = Offset(coralLeft, coralTop),
                        end = Offset(coralLeft + coralW, coralTop + coralH)
                    ),
                    topLeft = Offset(coralLeft, coralTop),
                    size = Size(coralW, coralH),
                    cornerRadius = CornerRadius(44f, 44f)
                )
            }

            // Primary Hero Yellow Ribbed Hardshell Suitcase tilted on right
            rotate(degrees = 9f, pivot = Offset(w * 0.68f, h * 0.56f)) {
                val suitW = w * 0.56f
                val suitH = h * 0.34f
                val suitLeft = w * 0.40f
                val suitTop = h * 0.36f

                // Silver Telescopic Handle Bars
                val handleLeft = suitLeft + suitW * 0.32f
                val handleRight = suitLeft + suitW * 0.68f
                val handleTop = suitTop - h * 0.095f
                drawRoundRect(
                    color = Color(0xFFD4D4D8),
                    topLeft = Offset(handleLeft, handleTop),
                    size = Size(12f, h * 0.11f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFD4D4D8),
                    topLeft = Offset(handleRight, handleTop),
                    size = Size(12f, h * 0.11f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFFE4E4E7),
                    topLeft = Offset(handleLeft - 8f, handleTop - 10f),
                    size = Size((handleRight - handleLeft) + 28f, 20f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // 3D Depth Back Shell
                drawRoundRect(
                    color = Color(0xFFB45309),
                    topLeft = Offset(suitLeft + 18f, suitTop + 14f),
                    size = Size(suitW, suitH),
                    cornerRadius = CornerRadius(52f, 52f)
                )

                // Main Golden-Yellow Hardshell Body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFDE047),
                            SuitcaseYellow,
                            SuitcaseAmber
                        ),
                        start = Offset(suitLeft, suitTop),
                        end = Offset(suitLeft + suitW, suitTop + suitH)
                    ),
                    topLeft = Offset(suitLeft, suitTop),
                    size = Size(suitW, suitH),
                    cornerRadius = CornerRadius(52f, 52f)
                )

                // Concentric Molded Ribs (matching the yellow suitcase in the reference image)
                for (ring in 1..3) {
                    val inset = ring * 24f
                    drawRoundRect(
                        color = Color(0xFFCA8A04).copy(alpha = 0.45f),
                        topLeft = Offset(suitLeft + inset, suitTop + inset),
                        size = Size(suitW - inset * 2, suitH - inset * 2),
                        cornerRadius = CornerRadius(38f - ring * 6f, 38f - ring * 6f),
                        style = Stroke(width = 5f)
                    )
                }

                // Black Security Luggage Strap with Buckle & White Tag
                val strapY = suitTop + suitH * 0.40f
                val strapH = suitH * 0.13f
                drawRect(
                    color = Color(0xFF18181B),
                    topLeft = Offset(suitLeft - 4f, strapY),
                    size = Size(suitW + 14f, strapH)
                )
                // Buckle
                drawRoundRect(
                    color = Color(0xFF27272A),
                    topLeft = Offset(suitLeft + suitW * 0.28f, strapY - 4f),
                    size = Size(suitW * 0.22f, strapH + 8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // White ID Tag on Strap
                drawRoundRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset(suitLeft + suitW * 0.56f, strapY + 6f),
                    size = Size(suitW * 0.20f, strapH - 12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Four Dual-Caster Yellow & Black Spinner Wheels
                val wheelPositions = listOf(
                    Offset(suitLeft + suitW * 0.12f, suitTop + suitH + 16f),
                    Offset(suitLeft + suitW * 0.34f, suitTop + suitH + 26f),
                    Offset(suitLeft + suitW * 0.68f, suitTop + suitH + 18f),
                    Offset(suitLeft + suitW * 0.88f, suitTop + suitH + 30f)
                )
                wheelPositions.forEach { pos ->
                    drawCircle(
                        color = Color(0xFF18181B),
                        radius = 24f,
                        center = pos
                    )
                    drawCircle(
                        color = SuitcaseYellow,
                        radius = 13f,
                        center = Offset(pos.x - 4f, pos.y - 2f)
                    )
                }
            }
        }

        // Foreground Typography & Bottom "Get Started" Pill Button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.padding(top = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to our\nLuggage\nTracker App!",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.testTag("intro_headline")
                )

                Text(
                    text = "Track your belongings with ease,\nanytime, anywhere.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = TextSecondary
                )
            }

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("get_started_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeChartreuse,
                    contentColor = TextOnLime
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = TextOnLime
                )
            }
        }
    }
}

/**
 * Screen 2: Login / Sign-Up Screen matching the top-right panel of the user's reference image:
 * "Ready to keep track of your luggage? Please log in to access your account."
 * Provides:
 * - Login / Sign Up mode toggle
 * - "Login To Account" / "Sign Up with Google" primary lime-yellow pill button (using Jetpack CredentialManager)
 * - "Continue As Guest" secondary white pill button
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

    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A5030),
                        Color(0xFF6B743E),
                        Color(0xFF939F4D)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Decorative Conveyor Belt & Suitcases Background Canvas (matching top-right reference screen)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Diagonal Airport Carousel Track Lines
            for (i in 0..8) {
                val offset = i * (w * 0.16f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, offset * 0.6f),
                    end = Offset(w, offset * 0.6f + h * 0.22f),
                    strokeWidth = 3f
                )
            }

            // Top-left Yellow Suitcase on Carousel
            rotate(degrees = -18f, pivot = Offset(w * 0.25f, h * 0.18f)) {
                drawRoundRect(
                    color = SuitcaseYellow.copy(alpha = 0.35f),
                    topLeft = Offset(w * 0.05f, h * 0.06f),
                    size = Size(w * 0.42f, h * 0.20f),
                    cornerRadius = CornerRadius(32f, 32f)
                )
            }

            // Top-right Amber Suitcase on Carousel
            rotate(degrees = -18f, pivot = Offset(w * 0.78f, h * 0.14f)) {
                drawRoundRect(
                    color = SuitcaseCoral.copy(alpha = 0.30f),
                    topLeft = Offset(w * 0.58f, h * 0.03f),
                    size = Size(w * 0.38f, h * 0.18f),
                    cornerRadius = CornerRadius(28f, 28f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Back Button + Login / Sign Up Switcher Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToIntro,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .testTag("auth_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Introduction",
                        tint = Color.White
                    )
                }

                // Login / Sign Up Mode Toggle Pill
                Surface(
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .testTag("mode_login_tab")
                                .clickable {
                                    isSignUpMode = false
                                    authErrorMessage = null
                                },
                            color = if (!isSignUpMode) LimeChartreuse else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Log In",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!isSignUpMode) TextOnLime else Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .testTag("mode_signup_tab")
                                .clickable {
                                    isSignUpMode = true
                                    authErrorMessage = null
                                },
                            color = if (isSignUpMode) LimeChartreuse else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSignUpMode) TextOnLime else Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Headline & Subheadline from Reference Design
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isSignUpMode) {
                        "Create your\nluggage tracker\naccount"
                    } else {
                        "Ready to keep\ntrack of your\nluggage?"
                    },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.testTag("auth_headline")
                )

                Text(
                    text = if (isSignUpMode) {
                        "Sign up with your Google account for automatic cloud manifest backup."
                    } else {
                        "Please log in to access your account."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Feature Highlights Card + Primary Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = LimeChartreuse,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isSignUpMode) {
                                    "Instant One-Tap Google Account Registration"
                                } else {
                                    "Cloud Sync & Real-Time SAM 3.1 Manifest"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Your scanned gear weights, airline allowance presets, and personal bag shifts stay synced securely with Google Sign-In.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }

                AnimatedVisibility(visible = authErrorMessage != null) {
                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = authErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // Primary Lime-Yellow Pill Button: "Login To Account" / "Sign Up With Google"
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
                        .height(58.dp)
                        .testTag("login_to_account_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeChartreuse,
                        contentColor = TextOnLime,
                        disabledContainerColor = LimeChartreuse.copy(alpha = 0.6f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextOnLime,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUpMode) "Sign Up with Google" else "Login To Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextOnLime
                        )
                    }
                }

                // Secondary White Pill Button: "Continue As Guest"
                Button(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("continue_as_guest_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue As Guest",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

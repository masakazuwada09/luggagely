package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.googleProfilePhotoUrlFlow
import com.example.auth.signOutUser
import com.example.data.BaggageAsyncStorage
import com.example.data.BaggageRepository
import com.example.model.BaggageGuardViewModel
import com.example.ui.BaggageGuardStudio
import com.example.ui.screens.AuthLoginSignUpScreen
import com.example.ui.screens.IntroWelcomeScreen
import com.example.ui.theme.BaggageGuardTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AsyncStorage for SAM segmented item cutouts and local item manifest
        BaggageAsyncStorage.init(this)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            BaggageGuardTheme {
                LuggageTrackerAppRoot()
            }
        }
    }
}

private enum class AppStage {
    INTRO,
    AUTH,
    STUDIO
}

@Composable
private fun LuggageTrackerAppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val sessionPrefs = remember(context) {
        context.getSharedPreferences("luggagely_session_prefs", android.content.Context.MODE_PRIVATE)
    }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val credentialPhotoUrl by googleProfilePhotoUrlFlow.collectAsStateWithLifecycle()

    var isGuestSession by rememberSaveable {
        mutableStateOf(sessionPrefs.getBoolean("is_guest_session", false))
    }

    var stage by rememberSaveable {
        val initialStage = when {
            auth.currentUser != null -> AppStage.STUDIO
            sessionPrefs.getBoolean("is_guest_session", false) -> AppStage.STUDIO
            sessionPrefs.getString("last_stage", null) == AppStage.STUDIO.name && auth.currentUser != null -> AppStage.STUDIO
            sessionPrefs.getString("last_stage", null) == AppStage.AUTH.name -> AppStage.AUTH
            else -> AppStage.INTRO
        }
        mutableStateOf(initialStage)
    }

    // Whenever a user is signed in (including after returning from background or credential picker),
    // always redirect directly to STUDIO (Dashboard) and never show INTRO.
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isGuestSession = false
            stage = AppStage.STUDIO
            sessionPrefs.edit()
                .putBoolean("is_guest_session", false)
                .putString("last_stage", AppStage.STUDIO.name)
                .apply()
        }
    }

    // Persist current stage so backgrounding/reopening preserves place
    LaunchedEffect(stage, isGuestSession) {
        sessionPrefs.edit()
            .putBoolean("is_guest_session", isGuestSession)
            .putString("last_stage", stage.name)
            .apply()
    }

    Crossfade(targetState = stage, label = "root_stage_transition") { currentStage ->
        when (currentStage) {
            AppStage.INTRO -> {
                IntroWelcomeScreen(
                    onGetStarted = {
                        if (currentUser != null) {
                            stage = AppStage.STUDIO
                        } else {
                            stage = AppStage.AUTH
                        }
                    }
                )
            }

            AppStage.AUTH -> {
                BackHandler {
                    stage = AppStage.INTRO
                }
                LaunchedEffect(Unit) {
                    attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isGuestSession = false
                            stage = AppStage.STUDIO
                        },
                        onUnauthenticated = {},
                        scope = coroutineScope
                    )
                }
                AuthLoginSignUpScreen(
                    onAuthSuccess = {
                        isGuestSession = false
                        stage = AppStage.STUDIO
                    },
                    onContinueAsGuest = {
                        isGuestSession = true
                        stage = AppStage.STUDIO
                    },
                    onBackToIntro = {
                        stage = AppStage.INTRO
                    }
                )
            }

            AppStage.STUDIO -> {
                BackHandler {
                    if (currentUser == null) {
                        isGuestSession = false
                        stage = AppStage.AUTH
                    }
                }
                val activeUser = currentUser
                val activeUid = activeUser?.uid
                if (activeUid != null) {
                    // Authenticated session: instantiate ViewModel backed by Firestore BaggageRepository
                    val studioViewModel: BaggageGuardViewModel = viewModel(
                        key = "auth_$activeUid",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                BaggageGuardViewModel(
                                    repository = BaggageRepository(db),
                                    currentUserId = activeUid
                                )
                            }
                        }
                    )
                    val displayLabel = activeUser.displayName?.takeIf { it.isNotBlank() }
                        ?: activeUser.email?.substringBefore("@")
                            ?.replace(".", " ")
                            ?.replace("_", " ")
                            ?.replaceFirstChar { it.uppercase() }
                        ?: "Signed-In Traveler"
                    val emailAddress = activeUser.email?.takeIf { it.isNotBlank() } ?: ""
                    val photoUrl = activeUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
                        ?: credentialPhotoUrl

                    BaggageGuardStudio(
                        viewModel = studioViewModel,
                        userLabel = displayLabel,
                        userEmail = emailAddress,
                        userPhotoUrl = photoUrl,
                        isGoogleSignedIn = true,
                        onNavigateBackOrSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    isGuestSession = false
                                    sessionPrefs.edit()
                                        .putBoolean("is_guest_session", false)
                                        .putString("last_stage", AppStage.AUTH.name)
                                        .apply()
                                    stage = AppStage.AUTH
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                } else {
                    // Guest session (local state)
                    val guestViewModel: BaggageGuardViewModel = viewModel(key = "guest_session")
                    BaggageGuardStudio(
                        viewModel = guestViewModel,
                        userLabel = "Guest Traveler",
                        userEmail = "",
                        userPhotoUrl = null,
                        isGoogleSignedIn = false,
                        onNavigateBackOrSignOut = {
                            isGuestSession = false
                            sessionPrefs.edit()
                                .putBoolean("is_guest_session", false)
                                .putString("last_stage", AppStage.AUTH.name)
                                .apply()
                            stage = AppStage.AUTH
                        }
                    )
                }
            }
        }
    }
}

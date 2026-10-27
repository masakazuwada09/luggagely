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
import com.example.auth.signOutUser
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

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var stage by rememberSaveable { mutableStateOf(AppStage.INTRO) }
    var isGuestSession by rememberSaveable { mutableStateOf(false) }

    // If user authenticates with Google on the AUTH screen, advance to STUDIO
    LaunchedEffect(currentUser) {
        if (currentUser != null && stage == AppStage.AUTH) {
            stage = AppStage.STUDIO
        }
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
                    stage = AppStage.AUTH
                }
                val activeUid = currentUser?.uid
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
                    val displayLabel = currentUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser?.email?.substringBefore("@")
                        ?: "Cloud Synced"

                    BaggageGuardStudio(
                        viewModel = studioViewModel,
                        userLabel = displayLabel,
                        onNavigateBackOrSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    isGuestSession = false
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
                        userLabel = if (isGuestSession) "Guest Mode" else "Log In",
                        onNavigateBackOrSignOut = {
                            stage = AppStage.AUTH
                        }
                    )
                }
            }
        }
    }
}

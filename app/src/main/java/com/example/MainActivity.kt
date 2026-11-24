package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.LocalPreferencesRepository
import com.example.data.repository.SaniAiRepository
import com.example.data.repository.SaniFirestoreRepository
import com.example.data.themes.ThemesCatalog
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.services.SaniLocationHelper
import com.example.services.SaniVoiceManager
import com.example.ui.components.FloatingParticlesBackground
import com.example.ui.components.SaniFloatingBottomBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.AutoReplyDialog
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreatorHubDialog
import com.example.ui.screens.DocumentsDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageDialog
import com.example.ui.screens.PhoneControlDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemesStudioScreen
import com.example.ui.theme.SaniAssistantTheme
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.SaniViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

const val ROUTE_HOME = "home"
const val ROUTE_CHAT = "chat"
const val ROUTE_CAMERA = "camera"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_THEMES_STUDIO = "themes_studio"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring before Compose UI / ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )
    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (!autoSignInChecked) {
        SaniAssistantTheme {
            FloatingParticlesBackground {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    } else if (user == null) {
        SaniAssistantTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedSaniApp(
                currentUserId = user.uid,
                defaultName = user.displayName ?: "Creator",
                defaultPhoto = user.photoUrl?.toString().orEmpty()
            )
        }
    }
}

@Composable
private fun AuthenticatedSaniApp(
    currentUserId: String,
    defaultName: String,
    defaultPhoto: String
) {
    val viewModel: SaniViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SaniViewModel(
                    firestoreRepo = SaniFirestoreRepository(app),
                    localPrefsRepo = LocalPreferencesRepository(app),
                    aiRepo = SaniAiRepository(),
                    voiceManager = SaniVoiceManager(app),
                    locationHelper = SaniLocationHelper(app),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultName,
                    defaultPhotoUrl = defaultPhoto
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val autoReplySettings by viewModel.autoReplySettings.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    val profile = (profileState as? ProfileUiState.Ready)?.profile
        ?: com.example.data.model.UserProfile(userId = currentUserId, displayName = defaultName)

    val activeTheme = remember(profile.selectedThemeId) {
        ThemesCatalog.themeById(profile.selectedThemeId)
    }
    val activeOrb = remember(profile.selectedOrbId) {
        ThemesCatalog.orbById(profile.selectedOrbId)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingChatPrompt by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAutoReplyDialog by remember { mutableStateOf(false) }
    var showCreatorHubDialog by remember { mutableStateOf(false) }
    var showDocumentsDialog by remember { mutableStateOf(false) }
    var showPhoneControlDialog by remember { mutableStateOf(false) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startHomeVoiceAssistant()
        else viewModel.showToast("Microphone permission required for voice orb.")
    }

    LaunchedEffect(statusToast) {
        statusToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    SaniAssistantTheme(themeConfig = activeTheme) {
        FloatingParticlesBackground {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    val bottomTabRoute = when (currentRoute) {
                        ROUTE_THEMES_STUDIO -> ROUTE_SETTINGS
                        else -> currentRoute
                    }
                    SaniFloatingBottomBar(
                        currentRoute = bottomTabRoute,
                        orbPrimaryColor = activeOrb.primaryColor,
                        orbSecondaryColor = activeOrb.secondaryColor,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        audioLevel = audioLevel,
                        onNavigate = { targetRoute ->
                            if (currentRoute != targetRoute) {
                                navController.navigate(targetRoute) {
                                    popUpTo(ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onOrbClick = {
                            if (isListening || isSpeaking) {
                                viewModel.stopVoiceInputOrSpeech()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) viewModel.startHomeVoiceAssistant()
                                else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp)
                ) {
                    if (profileState is ProfileUiState.Error) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = (profileState as ProfileUiState.Error).message,
                                color = activeTheme.primaryText
                            )
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onOpenChat = { prompt ->
                                        pendingChatPrompt = prompt
                                        navController.navigate(ROUTE_CHAT) {
                                            popUpTo(ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenCamera = {
                                        navController.navigate(ROUTE_CAMERA) {
                                            popUpTo(ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenThemesStudio = {
                                        navController.navigate(ROUTE_THEMES_STUDIO)
                                    },
                                    onOpenCreatorHub = { showCreatorHubDialog = true },
                                    onOpenDocuments = { showDocumentsDialog = true },
                                    onOpenPhoneControl = { showPhoneControlDialog = true },
                                    onOpenSettings = {
                                        navController.navigate(ROUTE_SETTINGS) {
                                            popUpTo(ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_CHAT) {
                                BackHandler {
                                    navController.navigate(ROUTE_HOME) {
                                        popUpTo(ROUTE_HOME) { inclusive = true }
                                    }
                                }
                                ChatScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    initialPrompt = pendingChatPrompt,
                                    onConsumedInitialPrompt = { pendingChatPrompt = null }
                                )
                            }

                            composable(ROUTE_CAMERA) {
                                BackHandler {
                                    navController.navigate(ROUTE_HOME) {
                                        popUpTo(ROUTE_HOME) { inclusive = true }
                                    }
                                }
                                CameraVisionScreen(viewModel = viewModel)
                            }

                            composable(ROUTE_SETTINGS) {
                                BackHandler {
                                    navController.navigate(ROUTE_HOME) {
                                        popUpTo(ROUTE_HOME) { inclusive = true }
                                    }
                                }
                                SettingsScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onOpenCreatorHub = { showCreatorHubDialog = true },
                                    onOpenThemesStudio = {
                                        navController.navigate(ROUTE_THEMES_STUDIO)
                                    },
                                    onOpenLanguageDialog = { showLanguageDialog = true },
                                    onOpenAutoReplyDialog = { showAutoReplyDialog = true },
                                    onOpenPhoneControl = { showPhoneControlDialog = true }
                                )
                            }

                            composable(ROUTE_THEMES_STUDIO) {
                                BackHandler { navController.popBackStack() }
                                ThemesStudioScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    if (showLanguageDialog) {
                        LanguageDialog(
                            currentLanguageCode = profile.languageCode,
                            onDismiss = { showLanguageDialog = false },
                            onSave = { code ->
                                viewModel.updateProfileField(mapOf("languageCode" to code))
                                showLanguageDialog = false
                            }
                        )
                    }

                    if (showAutoReplyDialog) {
                        AutoReplyDialog(
                            initialSettings = autoReplySettings,
                            onDismiss = { showAutoReplyDialog = false },
                            onSave = { newSettings ->
                                viewModel.saveAutoReplySettings(newSettings)
                                showAutoReplyDialog = false
                            }
                        )
                    }

                    if (showCreatorHubDialog) {
                        CreatorHubDialog(
                            viewModel = viewModel,
                            profile = profile,
                            onDismiss = { showCreatorHubDialog = false }
                        )
                    }

                    if (showDocumentsDialog) {
                        DocumentsDialog(
                            viewModel = viewModel,
                            onDismiss = { showDocumentsDialog = false }
                        )
                    }

                    if (showPhoneControlDialog) {
                        PhoneControlDialog(
                            viewModel = viewModel,
                            onDismiss = { showPhoneControlDialog = false }
                        )
                    }
                }
            }
        }
    }
}

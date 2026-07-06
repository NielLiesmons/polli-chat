package com.polli.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polli.ui.components.DetailScreenHeader
import com.polli.ui.components.HomeBackHandler
import com.polli.ui.components.PolliGhostButton
import com.polli.ui.components.PolliPrimaryButton
import com.polli.ui.components.ProfileAvatar
import com.polli.ui.components.RoundBackButton
import com.polli.ui.components.SelfAvatar
import com.polli.ui.home.HomeNote
import com.polli.ui.screens.AccountSetupScreen
import com.polli.ui.screens.ArchiveScreen
import com.polli.ui.screens.HomeScreen
import com.polli.ui.screens.QrPasteDialog
import com.polli.ui.screens.WelcomeScreen
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens
import com.polli.ui.theme.LabTheme
import com.polli.ui.theme.accent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class OnboardingStep {
    data object Welcome : OnboardingStep()

    data object AccountSetup : OnboardingStep()

    data class QrImport(val linkSecondDevice: Boolean) : OnboardingStep()
}

private sealed class MainRoute {
    data object Home : MainRoute()

    data object Archive : MainRoute()

    data object Profiles : MainRoute()

    data object NewConversation : MainRoute()

    data class Chat(val chatId: Int) : MainRoute()

    data class NoteEditor(val msgId: Int?) : MainRoute()
}

@Composable
fun PolliDesktopApp(engine: DesktopEngine) {
    val prefs = remember { DesktopUiPreferences() }
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    val backStack = remember { mutableStateListOf<MainRoute>(MainRoute.Home) }
    var onboardingStep by remember {
        mutableStateOf<OnboardingStep?>(if (engine.needsOnboarding) OnboardingStep.Welcome else null)
    }
    var setupBusy by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    var advancedEmail by remember { mutableStateOf("") }
    var advancedPassword by remember { mutableStateOf("") }

    fun navigate(to: MainRoute) {
        backStack.add(to)
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    DisposableEffect(engine) {
        val handle = engine.repository.observeInbox { refreshTick++ }
        onDispose { handle.close() }
    }

    LabTheme(prefs = prefs) {
        when (val step = onboardingStep) {
            is OnboardingStep.Welcome ->
                WelcomeScreen(
                    onCreateAccount = { onboardingStep = OnboardingStep.AccountSetup },
                    onImportQr = { onboardingStep = OnboardingStep.QrImport(linkSecondDevice = false) },
                    onLinkSecondDevice = { onboardingStep = OnboardingStep.QrImport(linkSecondDevice = true) },
                    onAdvancedSetup = { showAdvanced = true },
                )
            is OnboardingStep.AccountSetup ->
                AccountSetupScreen(
                    initialDisplayName = engine.initialDisplayName(),
                    externalBusy = setupBusy,
                    onBack = { onboardingStep = OnboardingStep.Welcome },
                    onCreate = { name ->
                        if (!engine.canUseEngine) {
                            setupError = engine.statusMessage
                            return@AccountSetupScreen
                        }
                        setupBusy = true
                        setupError = null
                        scope.launch {
                            val result =
                                withContext(Dispatchers.IO) {
                                    engine.createAccountWithDisplayName(name)
                                }
                            setupBusy = false
                            if (result.isSuccess) {
                                onboardingStep = null
                                refreshTick++
                            } else {
                                setupError = result.exceptionOrNull()?.message ?: "Configuration failed"
                            }
                        }
                    },
                )
            is OnboardingStep.QrImport -> {
                if (setupBusy && step.linkSecondDevice) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accent().solid)
                            Text(
                                "Transferring backup…\nKeep both devices on the same Wi‑Fi network.",
                                color = LabColors.White33,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                } else {
                    QrPasteDialog(
                        title = if (step.linkSecondDevice) "Link second device" else "Scan QR / import",
                        hint =
                            if (step.linkSecondDevice) {
                                "Paste the backup QR from your other device. Both devices should be on the same network."
                            } else {
                                "Paste QR code content"
                            },
                        onDismiss = { onboardingStep = OnboardingStep.Welcome },
                        onSubmit = { qr ->
                            if (!engine.canUseEngine) {
                                setupError = engine.statusMessage
                                onboardingStep = OnboardingStep.Welcome
                                return@QrPasteDialog
                            }
                            setupBusy = true
                            setupError = null
                            scope.launch {
                                val result =
                                    withContext(Dispatchers.IO) {
                                        engine.handleQr(qr, step.linkSecondDevice)
                                    }
                                setupBusy = false
                                onboardingStep =
                                    when (result) {
                                        DesktopEngine.QrHandleResult.Done -> {
                                            refreshTick++
                                            null
                                        }
                                        is DesktopEngine.QrHandleResult.NeedDisplayName -> {
                                            OnboardingStep.AccountSetup
                                        }
                                        is DesktopEngine.QrHandleResult.Failed -> {
                                            setupError = result.message
                                            OnboardingStep.Welcome
                                        }
                                    }
                            }
                        },
                    )
                }
            }
            null -> {
                @Suppress("UNUSED_VARIABLE")
                val tick = refreshTick
                val route = backStack.last()
                HomeBackHandler(enabled = backStack.size > 1) { pop() }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (route) {
                        MainRoute.Home ->
                            HomeScreen(
                                profileName = engine.initialDisplayName().ifBlank { "Profile" },
                                profileSeed = engine.profileSeed().ifBlank { "me" },
                                chatRepository = engine.repository,
                                notes = emptyList<HomeNote>(),
                                onProfileClick = { navigate(MainRoute.Profiles) },
                                onPlusClick = { navigate(MainRoute.NewConversation) },
                                onChatClick = { navigate(MainRoute.Chat(it)) },
                                onChannelClick = { navigate(MainRoute.Chat(it)) },
                                onArchiveClick = { navigate(MainRoute.Archive) },
                                onNewNote = { navigate(MainRoute.NoteEditor(msgId = null)) },
                                onOpenNote = { navigate(MainRoute.NoteEditor(msgId = it)) },
                                chatAvatar = { item, size ->
                                    ProfileAvatar(name = item.name, seed = item.colorSeed, size = size)
                                },
                                selfAvatar = { name, size, onClick ->
                                    SelfAvatar(
                                        name = name,
                                        size = size,
                                        onClick = onClick,
                                        sigilIdentity = engine.profileSeed().ifBlank { name },
                                    )
                                },
                            )
                        MainRoute.Archive -> {
                            val nowSec = System.currentTimeMillis() / 1000
                            ArchiveScreen(
                                items = engine.repository.loadArchived(),
                                nowSec = nowSec,
                                onBack = { pop() },
                                onOpenChat = { navigate(MainRoute.Chat(it)) },
                                backButton = { RoundBackButton(onClick = { pop() }) },
                                avatar = { item ->
                                    ProfileAvatar(name = item.name, seed = item.colorSeed, size = 44.dp)
                                },
                            )
                        }
                        MainRoute.Profiles ->
                            PlaceholderScreen(
                                title = "Profiles",
                                body = "Account & appearance settings — coming to desktop next.",
                                onBack = { pop() },
                            )
                        MainRoute.NewConversation ->
                            PlaceholderScreen(
                                title = "New conversation",
                                body = "Contact picker & group create — coming to desktop next.",
                                onBack = { pop() },
                            )
                        is MainRoute.Chat ->
                            PlaceholderScreen(
                                title = "Chat #${route.chatId}",
                                body = "Full chat feed + composer via RPC engine — next slice.",
                                onBack = { pop() },
                            )
                        is MainRoute.NoteEditor ->
                            PlaceholderScreen(
                                title = if (route.msgId == null) "New note" else "Note #${route.msgId}",
                                body = "Notes editor via self-talk chat — coming to desktop next.",
                                onBack = { pop() },
                            )
                    }
                }
            }
        }

        setupError?.let { message ->
            Text(
                text = message,
                color = LabColors.Rouge,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (showAdvanced) {
            AdvancedSetupDialog(
                email = advancedEmail,
                password = advancedPassword,
                onEmailChange = { advancedEmail = it },
                onPasswordChange = { advancedPassword = it },
                onDismiss = { showAdvanced = false },
                onManualLogin = {
                    if (!engine.canUseEngine) {
                        setupError = engine.statusMessage
                        showAdvanced = false
                        return@AdvancedSetupDialog
                    }
                    setupBusy = true
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                engine.configureManualLogin(advancedEmail.trim(), advancedPassword)
                            }
                        setupBusy = false
                        showAdvanced = false
                        if (result.isSuccess) {
                            onboardingStep = null
                            refreshTick++
                        } else {
                            setupError = result.exceptionOrNull()?.message
                        }
                    }
                },
                onUseMock = {
                    engine.useMock()
                    showAdvanced = false
                    onboardingStep = null
                    refreshTick++
                },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailScreenHeader(
            title = title,
            backButton = { RoundBackButton(onClick = onBack) },
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = body,
                color = LabColors.White33,
                modifier = Modifier.padding(horizontal = LabDimens.HomeBarPadding),
            )
        }
    }
}

@Composable
private fun AdvancedSetupDialog(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onManualLogin: () -> Unit,
    onUseMock: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced setup", color = LabColors.White85) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PolliGhostButton(
                label = "Manual login",
                onClick = onManualLogin,
                color = if (email.isNotBlank() && password.isNotBlank()) LabColors.White85 else LabColors.White16,
            )
        },
        dismissButton = {
            PolliGhostButton(label = "Preview with mock data", onClick = onUseMock)
        },
    )
}

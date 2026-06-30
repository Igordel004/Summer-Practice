package ru.ritg.messengerclient.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ritg.messengerclient.screen.ChatScreen
import ru.ritg.messengerclient.screen.ChatsScreen
import ru.ritg.messengerclient.screen.ContactsScreen
import ru.ritg.messengerclient.screen.LoginScreen
import ru.ritg.messengerclient.screen.OtpScreen
import ru.ritg.messengerclient.screen.ServerConfigScreen
import ru.ritg.messengerclient.ui.theme.DarkGray
import ru.ritg.messengerclient.ui.theme.Indigo
import ru.ritg.messengerclient.ui.theme.TextWhite
import ru.ritg.messengerclient.viewmodel.AuthViewModel
import ru.ritg.messengerclient.viewmodel.ChatViewModel
import ru.ritg.messengerclient.viewmodel.ChatsViewModel
import ru.ritg.messengerclient.viewmodel.ContactsViewModel

/**
 * Корневая навигация приложения.
 *
 * Определяет маршруты: login → otp → main → chat/{phone}.
 * Отображает баннер «Нет соединения» при недоступности сервера.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val chatsViewModel: ChatsViewModel = viewModel()
    val isConnected by authViewModel.isConnected.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isConnected) {
            Text(
                text = "Нет соединения с сервером",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFFD32F2F))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.weight(1f)
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToServerConfig = { navController.navigate("server_config") },
                    onNavigateToOtp = { navController.navigate("otp") },
                    onNavigateToContacts = { navController.navigate("main") }
                )
            }
            composable("server_config") {
                ServerConfigScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("otp") {
                OtpScreen(
                    viewModel = authViewModel,
                    onVerified = {
                        authViewModel.resetOtpState()
                        navController.navigate("main") {
                            popUpTo("login")
                        }
                    },
                    onBack = {
                        authViewModel.resetOtpState()
                        navController.popBackStack()
                    }
                )
            }
            composable("main") {
                MainScreen(
                    authViewModel = authViewModel,
                    contactsViewModel = contactsViewModel,
                    chatsViewModel = chatsViewModel,
                    chatViewModel = chatViewModel,
                    onOpenChat = { phone, partnerId ->
                        if (partnerId.isNotEmpty()) {
                            navController.navigate("chat/$phone/$partnerId")
                        }
                    },
                    onNavigateToServerConfig = {
                        navController.navigate("server_config")
                    },
                    onLogout = {
                        chatViewModel.disconnect()
                        authViewModel.logout()
                        chatsViewModel.clear()
                        chatViewModel.clear()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                "chat/{recipientPhone}/{partnerId}",
                arguments = listOf(
                    navArgument("recipientPhone") { type = NavType.StringType },
                    navArgument("partnerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val recipientPhone = backStackEntry.arguments?.getString("recipientPhone") ?: ""
                val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
                ChatScreen(
                    chatViewModel = chatViewModel,
                    authViewModel = authViewModel,
                    chatsViewModel = chatsViewModel,
                    contactsViewModel = contactsViewModel,
                    recipientPhone = recipientPhone,
                    partnerId = partnerId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Главный экран с вкладками «Чаты» и «Контакты».
 *
 * Управляет tab-навигацией, обновлением данных и WebSocket-соединением.
 *
 * @param authViewModel ViewModel авторизации
 * @param contactsViewModel ViewModel контактов
 * @param chatsViewModel ViewModel чатов
 * @param chatViewModel ViewModel текущего чата
 * @param onOpenChat callback перехода к чату
 * @param onNavigateToServerConfig переход к настройкам
 * @param onLogout callback выхода из аккаунта
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    authViewModel: AuthViewModel,
    contactsViewModel: ContactsViewModel,
    chatsViewModel: ChatsViewModel,
    chatViewModel: ChatViewModel,
    onOpenChat: (String, String) -> Unit,
    onNavigateToServerConfig: () -> Unit,
    onLogout: () -> Unit
) {
    val state by authViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Чаты", "Контакты")
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val soapClient = remember { ru.ritg.messengerclient.network.SoapClient() }

    val chatsUnauthorized by chatsViewModel.unauthorized.collectAsState()
    val contactsUnauthorized by contactsViewModel.unauthorized.collectAsState()

    LaunchedEffect(chatsUnauthorized, contactsUnauthorized) {
        if (chatsUnauthorized || contactsUnauthorized) {
            contactsViewModel.clearUnauthorized()
            chatsViewModel.clearUnauthorized()
            onLogout()
        }
    }

    LaunchedEffect(Unit) {
        val s = authViewModel.state.value
        if (s.token.isNotEmpty()) {
            contactsViewModel.loadFromServerIfLoggedIn(s)
            chatsViewModel.loadChatsFromServer(s)
            chatViewModel.connectGlobal(s)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мессенджер") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray, titleContentColor = TextWhite, actionIconContentColor = TextWhite),
                actions = {
                    IconButton(onClick = {
                        val s = authViewModel.state.value
                        when (selectedTab) {
                            0 -> chatsViewModel.loadChatsFromServer(s)
                            1 -> contactsViewModel.refresh(s)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = onNavigateToServerConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Выход")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkGray,
                contentColor = TextWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Indigo
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) TextWhite else TextWhite.copy(alpha = 0.6f)) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    val contacts by contactsViewModel.contacts.collectAsState()
                    ChatsScreen(
                        chatsViewModel = chatsViewModel,
                        contacts = contacts,
                        onOpenChat = onOpenChat,
                        onSendMessage = { phone, message, nickname ->
                            if (chatsViewModel.conversations.value.any { it.phone == phone }) {
                                android.widget.Toast.makeText(context, "Чат уже существует", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val contact = contacts.find { it.phone == phone }
                                val senderId = state.userId
                                if (contact != null) {
                                    val recipientId = contact.userId
                                    if (recipientId != null && senderId != null) {
                                        coroutineScope.launch {
                                            chatViewModel.sendMessage(senderId, recipientId, message)
                                        }
                                    }
                                    val time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                    chatsViewModel.addOrUpdateConversation(
                                        phone = phone,
                                        nickname = nickname,
                                        lastMessage = message,
                                        timestamp = time
                                    )
                                    android.widget.Toast.makeText(context, "Сообщение отправлено", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            soapClient.configure(state.serverHost, state.serverPort)
                                            soapClient.findUserByPhone(state.token, phone)
                                        }
                                        result.onSuccess { xml ->
                                            val found = soapClient.extractTag(xml, "found")
                                            if (found == "true") {
                                                val userId = soapClient.extractTag(xml, "userId")
                                                val userNickname = soapClient.extractTag(xml, "nickname")
                                                if (senderId != null) {
                                                    val recipientUuid = java.util.UUID.fromString(userId)
                                                    chatViewModel.sendMessage(senderId, recipientUuid, message)
                                                    val time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                                    chatsViewModel.addOrUpdateConversation(
                                                        phone = phone,
                                                        nickname = userNickname,
                                                        lastMessage = message,
                                                        timestamp = time
                                                    )
                                                    android.widget.Toast.makeText(context, "Сообщение отправлено", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                android.widget.Toast.makeText(context, "Пользователь не найден", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        result.onFailure {
                                            android.widget.Toast.makeText(context, "Ошибка поиска: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                1 -> ContactsScreen(
                    authViewModel = authViewModel,
                    contactsViewModel = contactsViewModel
                )
            }
        }
    }
}

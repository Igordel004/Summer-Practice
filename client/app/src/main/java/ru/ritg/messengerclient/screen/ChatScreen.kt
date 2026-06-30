package ru.ritg.messengerclient.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ritg.messengerclient.model.ChatMessage
import ru.ritg.messengerclient.ui.theme.DarkGray
import ru.ritg.messengerclient.ui.theme.TextWhite
import ru.ritg.messengerclient.viewmodel.AuthViewModel
import ru.ritg.messengerclient.viewmodel.ChatViewModel
import ru.ritg.messengerclient.viewmodel.ChatsViewModel
import ru.ritg.messengerclient.viewmodel.ContactsViewModel
import java.util.UUID

/**
 * Экран чата с собеседником.
 *
 * Загружает историю переписки, отображает список сообщений,
 * позволяет отправлять, редактировать и удалять сообщения через WebSocket.
 *
 * @param chatViewModel ViewModel чата
 * @param authViewModel ViewModel авторизации
 * @param chatsViewModel ViewModel списка чатов
 * @param contactsViewModel ViewModel контактов
 * @param recipientPhone номер телефона собеседника
 * @param onBack возврат на предыдущий экран
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    chatsViewModel: ChatsViewModel,
    contactsViewModel: ContactsViewModel,
    recipientPhone: String,
    onBack: () -> Unit
) {
    val state by authViewModel.state.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val contacts by contactsViewModel.contacts.collectAsState()
    val contact = remember(contacts, recipientPhone) {
        contacts.find { it.phone == recipientPhone }
    }
    val contactNickname = contact?.nickname ?: ""

    val connected by chatViewModel.connected.collectAsState()
    val isLoadingMore by chatViewModel.isLoadingMore.collectAsState()
    val hasMoreMessages by chatViewModel.hasMoreMessages.collectAsState()

    LaunchedEffect(state.token, recipientPhone) {
        if (state.token.isNotEmpty()) {
            chatViewModel.setRecipient(recipientPhone)
            chatViewModel.clearMessages()
            chatsViewModel.addOrUpdateConversation(
                phone = recipientPhone,
                nickname = contactNickname
            )
            val recipientUserId = contact?.userId
            if (recipientUserId != null) {
                chatViewModel.loadHistory(state, recipientUserId, 0, 20) { history ->
                    chatViewModel.setMessages(history)
                    chatViewModel.markIncomingAsRead()
                }
            }
        }
    }

    LaunchedEffect(connected) {
        if (connected) {
            chatViewModel.markIncomingAsRead()
        }
    }

    var initialScrollComplete by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            initialScrollComplete = true
        }
    }

    LaunchedEffect(listState, initialScrollComplete) {
        if (!initialScrollComplete) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstVisibleItemIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            Triple(layoutInfo.totalItemsCount, firstVisibleItemIndex, hasMoreMessages)
        }.collect { (totalItemCount, firstVisibleItemIndex, hasMore) ->
            if (hasMore && totalItemCount > 0 && firstVisibleItemIndex <= 2 && !isLoadingMore) {
                chatViewModel.loadMoreMessages(state)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val s = authViewModel.state.value
            if (s.token.isNotEmpty()) {
                chatsViewModel.loadChatsFromServer(s)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (contactNickname.isNotBlank()) "$contactNickname | $recipientPhone" else recipientPhone,
                        fontSize = 16.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray, titleContentColor = TextWhite, navigationIconContentColor = TextWhite),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 8.dp)
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus()
                        editingMessage = null
                        inputText = ""
                    },
                    onLongClick = {}
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        message = msg,
                        onEdit = {
                            editingMessage = msg
                            inputText = msg.payload
                        },
                        onDelete = {
                            if (msg.id != null) {
                                chatViewModel.deleteMessage(msg.id)
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (editingMessage != null) "Редактировать..." else "Сообщение...") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val senderId = state.userId
                            val recipientId = contact?.userId
                            if (recipientId == null || senderId == null) return@IconButton

                            val editing = editingMessage
                            if (editing != null && editing.id != null) {
                                chatViewModel.editMessage(editing.id, inputText)
                                editingMessage = null
                            } else {
                                val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                chatViewModel.sendMessage(
                                    senderId = senderId,
                                    recipientId = recipientId,
                                    payload = inputText
                                )
                                chatsViewModel.addOrUpdateConversation(
                                    phone = recipientPhone,
                                    nickname = contactNickname,
                                    lastMessage = inputText,
                                    timestamp = timestamp
                                )
                            }
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = if (editingMessage != null) "Сохранить" else "Отправить")
                }
            }
        }
    }
}

/**
 * Пузырёк сообщения в чате.
 *
 * Отображает текст, время и статус доставки.
 * Долгое нажатие на своё сообщение открывает выпадающее меню
 * с вариантами «Редактировать» и «Удалить».
 *
 * @param message сообщение для отображения
 * @param onEdit callback выбора «Редактировать»
 * @param onDelete callback выбора «Удалить»
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (message.isOwn) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = androidx.compose.ui.graphics.Color(0xFFE8E8E8)
    val shape = if (message.isOwn)
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    else
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgColor)
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { },
                    onLongClick = {
                        if (message.isOwn) {
                            showMenu = true
                        }
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.payload,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (message.status) {
                        "PENDING" -> "\u23F3"
                        "DELIVERED" -> "\u2713"
                        "READ" -> "\u2713\u2713"
                        else -> message.status
                    },
                    fontSize = 10.sp,
                    color = if (message.status == "READ") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Редактировать") },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

package ru.ritg.messengerclient.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.ritg.messengerclient.ui.theme.Indigo
import ru.ritg.messengerclient.model.Contact
import ru.ritg.messengerclient.viewmodel.ChatsViewModel

/**
 * Экран списка активных чатов.
 *
 * Отображает беседы с последними сообщениями. Кнопка «+» открывает
 * диалог нового чата с выбором контакта или вводом номера.
 *
 * @param chatsViewModel ViewModel чатов
 * @param contacts список контактов
 * @param onOpenChat callback перехода к чату
 * @param onSendMessage callback отправки первого сообщения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    chatsViewModel: ChatsViewModel,
    contacts: List<Contact>,
    onOpenChat: (String) -> Unit,
    onSendMessage: (phone: String, message: String, nickname: String) -> Unit
) {
    val conversations by chatsViewModel.conversations.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Indigo
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Нет чатов",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Нажмите + чтобы начать",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations) { convo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onOpenChat(convo.phone) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = if (convo.nickname.isNotBlank()) "${convo.nickname} | ${convo.phone}" else convo.phone,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (convo.lastMessage.isNotBlank()) {
                                        Text(
                                            text = convo.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (convo.timestamp.isNotBlank()) {
                                    Text(
                                        text = convo.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            NewChatDialog(
                contacts = contacts,
                onDismiss = { showDialog = false },
                onSend = { phone, message, nickname ->
                    onSendMessage(phone, message, nickname)
                    showDialog = false
                }
            )
        }
    }
}

/**
 * Диалог создания нового чата.
 *
 * Позволяет выбрать контакт из списка или ввести номер телефона вручную,
 * а также ввести текст первого сообщения.
 *
 * @param contacts список контактов
 * @param onDismiss закрытие диалога
 * @param onSend callback (номер телефона, сообщение, никнейм)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onSend: (phone: String, message: String, nickname: String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var newPhone by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый чат") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == 0, onClick = { mode = 0 })
                    Text("Номер телефона")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == 1, onClick = { mode = 1 })
                    Text("Контакт")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (mode == 0) {
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Номер телефона") },
                        placeholder = { Text("+79001234567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedContact?.let { c ->
                                if (c.nickname.isNotBlank()) "${c.nickname} | ${c.phone}" else c.phone
                            } ?: "",
                            onValueChange = {},
                            label = { Text("Выберите контакт") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            contacts.forEach { contact ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (contact.nickname.isNotBlank()) "${contact.nickname} | ${contact.phone}" else contact.phone)
                                    },
                                    onClick = {
                                        selectedContact = contact
                                        newPhone = contact.phone
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    label = { Text("Сообщение") },
                    placeholder = { Text("Текст сообщения...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val nickname = selectedContact?.nickname ?: ""
                    onSend(newPhone, newMessage, nickname)
                    newPhone = ""
                    newMessage = ""
                    selectedContact = null
                },
                enabled = newPhone.isNotBlank() && newMessage.isNotBlank()
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

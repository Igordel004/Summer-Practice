package ru.ritg.messengerclient.screen

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.ritg.messengerclient.ui.theme.Indigo
import ru.ritg.messengerclient.viewmodel.AuthViewModel
import ru.ritg.messengerclient.viewmodel.ContactsViewModel

/**
 * Экран управления контактами.
 *
 * Отображает список контактов с возможностью добавления и удаления.
 *
 * @param authViewModel ViewModel авторизации
 * @param contactsViewModel ViewModel контактов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    authViewModel: AuthViewModel,
    contactsViewModel: ContactsViewModel
) {
    val state by authViewModel.state.collectAsState()
    val contacts by contactsViewModel.contacts.collectAsState()
    val isLoading by contactsViewModel.isLoading.collectAsState()
    val message by contactsViewModel.message.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newPhone by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Indigo
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить контакт")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Нет контактов",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Нажмите + чтобы добавить",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
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
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(
                                            text = if (contact.nickname.isNotBlank()) "${contact.nickname} | ${contact.phone}" else contact.phone,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                                IconButton(onClick = { contactsViewModel.removeContact(state, contact) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (message != null) {
                Text(
                    text = message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
                contactsViewModel.clearMessage()
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Добавить контакт") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Номер телефона") },
                            placeholder = { Text("+79001234567") },
                            singleLine = true
                        )
                        if (isLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            contactsViewModel.addContact(state, newPhone)
                            newPhone = ""
                            showDialog = false
                        },
                        enabled = newPhone.isNotBlank() && !isLoading
                    ) {
                        Text("Добавить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        newPhone = ""
                    }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

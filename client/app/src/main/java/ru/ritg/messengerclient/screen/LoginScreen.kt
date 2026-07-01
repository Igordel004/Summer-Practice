package ru.ritg.messengerclient.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.ritg.messengerclient.ui.theme.DarkGray
import ru.ritg.messengerclient.ui.theme.Indigo
import ru.ritg.messengerclient.ui.theme.TextWhite
import ru.ritg.messengerclient.viewmodel.AuthViewModel

/**
 * Экран входа / регистрации.
 *
 * Позволяет ввести номер телефона и никнейм, запросить OTP-код.
 * Переключается между режимами «Вход» и «Регистрация».
 *
 * @param viewModel ViewModel авторизации
 * @param onNavigateToOtp переход к экрану верификации
 * @param onNavigateToContacts переход к списку контактов (при уже авторизованном токене)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToOtp: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    var phone by remember { mutableStateOf(state.phone) }
    var nickname by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(false) }

    if (state.token.isNotEmpty() && !otpSent) {
        onNavigateToContacts()
        return
    }

    if (otpSent) {
        onNavigateToOtp()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мессенджер") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray, titleContentColor = TextWhite, navigationIconContentColor = TextWhite, actionIconContentColor = TextWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) "Вход" else "Регистрация",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Номер телефона") },
                placeholder = { Text("+79001234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Никнейм") },
                    placeholder = { Text("Ваше имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { viewModel.requestOtp(phone, if (isLoginMode) "" else nickname) },
                enabled = !isLoading && phone.isNotBlank() && (isLoginMode || nickname.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo, contentColor = TextWhite)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Получить код")
                }
            }

            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isLoginMode) "Не зарегистрированы? " else "Уже зарегистрированы? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isLoginMode) "Регистрация" else "Вход",
                style = MaterialTheme.typography.bodyMedium,
                color = Indigo,
                modifier = Modifier.clickable {
                    isLoginMode = !isLoginMode
                    nickname = ""
                    viewModel.clearMessage()
                }
            )


        }
    }
}

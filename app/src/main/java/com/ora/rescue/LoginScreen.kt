package com.ora.rescue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

const val DEMO_LOGIN_ID = "demo@orarescue.app"
const val DEMO_LOGIN_PASSWORD = "OraDemo2026"

/** Local demonstration gate only. It intentionally does not claim server security. */
@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Rounded.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Text("Welcome to Ora Rescue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Sign in to open the local study-planning demo.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Login ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Button(
                onClick = {
                    if (email.trim().equals(DEMO_LOGIN_ID, ignoreCase = true) && password == DEMO_LOGIN_PASSWORD) onLogin()
                    else error = "Use the sample credentials shown below."
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            ) { Text("Sign in") }

            OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sample demo login", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Login ID: $DEMO_LOGIN_ID")
                    Text("Password: $DEMO_LOGIN_PASSWORD")
                    Text("Local demonstration only—no account or personal data is sent to a server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

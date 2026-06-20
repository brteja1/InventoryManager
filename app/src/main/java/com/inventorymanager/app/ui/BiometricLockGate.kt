package com.inventorymanager.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.inventorymanager.app.data.security.BiometricAuthenticator

@Composable
fun BiometricLockGate(
    onUnlocked: () -> Unit,
) {
    val activity = LocalContext.current as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val authenticator = remember { BiometricAuthenticator() }

    fun launchPrompt() {
        val host = activity
        if (host == null) {
            errorMessage = "This screen must be hosted by a FragmentActivity."
            return
        }

        authenticator.authenticate(
            activity = host,
            onSuccess = onUnlocked,
            onFailure = { message -> errorMessage = message },
        )
    }

    LaunchedEffect(Unit) {
        launchPrompt()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Inventory Locked",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Authenticate to access your private inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(onClick = { launchPrompt() }) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}

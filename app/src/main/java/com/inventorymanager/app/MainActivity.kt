package com.inventorymanager.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.inventorymanager.app.ui.BiometricLockGate
import com.inventorymanager.app.ui.InventoryManagerApp
import com.inventorymanager.app.ui.InventoryViewModel
import com.inventorymanager.app.ui.InventoryViewModelFactory
import com.inventorymanager.app.ui.theme.InventoryManagerTheme

class MainActivity : FragmentActivity() {
    private val inventoryViewModel: InventoryViewModel by lazy {
        val app = application as InventoryManagerApplication
        ViewModelProvider(
            this,
            InventoryViewModelFactory(
                app.container.inventoryRepository,
                app.container.imageStorageManager,
                app.container.imageEmbedderManager,
                app.container.backupManager,
            ),
        )[InventoryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InventoryManagerTheme {
                var unlocked by rememberSaveable { mutableStateOf(false) }

                if (!unlocked) {
                    BiometricLockGate(
                        onUnlocked = { unlocked = true },
                    )
                } else {
                    InventoryManagerApp(
                        viewModel = inventoryViewModel,
                        onLockApp = { unlocked = false },
                        onClearExportMessage = inventoryViewModel::clearBackupMessage,
                    )
                }
            }
        }
    }
}

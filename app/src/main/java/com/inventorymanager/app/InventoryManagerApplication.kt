package com.inventorymanager.app

import android.app.Application

class InventoryManagerApplication : Application() {
    val container: InventoryAppContainer by lazy {
        InventoryAppContainer(this)
    }
}

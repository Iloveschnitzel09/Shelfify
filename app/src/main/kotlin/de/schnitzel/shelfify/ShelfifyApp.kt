package de.schnitzel.shelfify

import android.app.Application
import de.schnitzel.shelfify.api.ConnectionGuard

class ShelfifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConnectionGuard.init(this)
    }
}
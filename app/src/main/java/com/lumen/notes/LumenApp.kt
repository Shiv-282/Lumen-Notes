package com.lumen.notes

import android.app.Application
import com.lumen.notes.data.AppGraph

class LumenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}


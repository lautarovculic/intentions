package com.lautarovculic.intentions.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.di.IntentionsApp

// Resolve the AppContainer from a composable.
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) {
        IntentionsApp.from(context.applicationContext as Application)
    }
}

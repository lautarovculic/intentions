package com.lautarovculic.intentions.di

import android.app.Application

class IntentionsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        fun from(app: Application): AppContainer = (app as IntentionsApp).container
    }
}

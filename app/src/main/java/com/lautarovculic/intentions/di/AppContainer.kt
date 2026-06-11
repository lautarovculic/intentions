package com.lautarovculic.intentions.di

import android.content.Context
import com.lautarovculic.intentions.core.intent.IntentDispatcher
import com.lautarovculic.intentions.core.root.RootDetector
import com.lautarovculic.intentions.core.root.RootExecutor
import com.lautarovculic.intentions.core.root.ShellRootExecutor
import com.lautarovculic.intentions.core.scanner.PackageScanner
import com.lautarovculic.intentions.core.storage.IntentionsDatabase
import com.lautarovculic.intentions.data.repository.AppRepository
import com.lautarovculic.intentions.data.repository.CaptureRepository
import com.lautarovculic.intentions.data.repository.IntentRepository
import com.lautarovculic.intentions.data.repository.NotesRepository
import com.lautarovculic.intentions.data.repository.RootRepository
import com.lautarovculic.intentions.core.model.IntentSpec
import kotlinx.coroutines.flow.MutableStateFlow

// Manual DI container, built once in IntentionsApp.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val rootExecutor: RootExecutor = ShellRootExecutor()
    private val rootDetector = RootDetector(rootExecutor)

    val database: IntentionsDatabase = IntentionsDatabase.get(appContext)

    val rootRepository = RootRepository(rootExecutor, rootDetector)
    val appRepository = AppRepository(PackageScanner(appContext), rootExecutor)

    private val intentDispatcher = IntentDispatcher(appContext, rootExecutor)
    val intentRepository = IntentRepository(
        dispatcher = intentDispatcher,
        runDao = database.intentRunDao(),
        presetDao = database.presetDao(),
    )

    val captureRepository = CaptureRepository(database.captureDao())
    val notesRepository = NotesRepository(database.noteDao())

    // whether the capture service is running
    val captureRunning = MutableStateFlow(false)

    // active capture session id
    val currentCaptureSession = MutableStateFlow<String?>(null)

    // cross-screen "Send to ..." handoffs: set the seed, navigate, target consumes it
    val builderHandoff = MutableStateFlow<IntentSpec?>(null)
    val fuzzerHandoff = MutableStateFlow<IntentSpec?>(null)
    val providerHandoff = MutableStateFlow<IntentSpec?>(null)

    fun sendToBuilder(spec: IntentSpec) { builderHandoff.value = spec }
    fun sendToFuzzer(spec: IntentSpec) { fuzzerHandoff.value = spec }
    fun sendToProvider(spec: IntentSpec) { providerHandoff.value = spec }
}

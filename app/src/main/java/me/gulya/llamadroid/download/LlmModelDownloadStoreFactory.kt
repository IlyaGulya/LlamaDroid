package me.gulya.llamadroid.download

import android.app.DownloadManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.nullableLong
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gulya.llamadroid.download.LlmModelDownloadStore.Intent
import me.gulya.llamadroid.download.LlmModelDownloadStore.Label
import me.gulya.llamadroid.download.LlmModelDownloadStore.State
import java.io.File

class LlmModelDownloadStoreFactory(
    private val storeFactory: StoreFactory,
    private val context: Context,
) {

    private val settings = SharedPreferencesSettings(
        context.getSharedPreferences("download", Context.MODE_PRIVATE)
    )

    fun create(llmModel: LlmModel): LlmModelDownloadStore =
        object : LlmModelDownloadStore,
            Store<Intent, State, Label> by storeFactory.create(
                name = "LlmModelDownloadStore",
                initialState = State(
                    llmModel,
                    null,
                    LlmModel.State.Initializing
                ),
                bootstrapper = SimpleBootstrapper(NextStep),
                executorFactory = ::ExecutorImpl,
                reducer = passthroughReducer(),
            ) {}

    private object NextStep

    private inner class ExecutorImpl :
        CoroutineExecutor<Intent, NextStep, State, State, Label>() {
        override fun executeIntent(intent: Intent) {
            when (intent) {
                Intent.Download -> executeAction(NextStep)
            }
        }

        override fun executeAction(action: NextStep) {
            val state = state()
            val model = state.model
            val downloadManager = context.getSystemService<DownloadManager>()!!
            var downloadId by settings.nullableLong("${model.id}_downloadId")
            val external = context.getExternalFilesDir("models")
            val modelFile = if (external != null) {
                ModelFile.ExternalFilesDir(
                    dir = external,
                    dirType = "models",
                    filename = model.filename
                )
            } else {
                ModelFile.InternalFilesDir(
                    dir = context.noBackupFilesDir.resolve("models"),
                    filename = model.filename,
                )
            }
            val localFile = modelFile.dir.resolve(model.filename)
            when (val downloadState = state.state) {
                LlmModel.State.Initializing -> {
                    scope.launch {
                        if (localFile.exists() && sha256(localFile) == model.sha256) {
                            dispatch(state.copy(state = LlmModel.State.Downloaded(localFile)))
                        } else {
                            if (downloadId != null) {
                                dispatch(
                                    state.copy(
                                        state = getDownloadState(
                                            downloadManager,
                                            downloadId!!
                                        ).toLlmState()
                                    ),
                                )
                            } else {
                                dispatch(state.copy(state = LlmModel.State.NotDownloaded(null)))
                            }
                        }
                    }
                }

                is LlmModel.State.Downloaded -> Unit
                is LlmModel.State.Downloading -> {
                    val query = DownloadManager.Query().setFilterById(downloadState.downloadId)

                    Napier.d {
                        downloadManager.query(query).use { cursor ->
                            cursor.moveToFirst()

                            """
                            Downloading ${model.name} ${downloadState.progress}%:
                            Status: ${cursor.getInt(DownloadManager.COLUMN_STATUS).asStatusString()}
                            Reason: ${cursor.getInt(DownloadManager.COLUMN_REASON).asReasonString()}
                            """.trimIndent()
                        }

                    }
                }

                is LlmModel.State.NotDownloaded -> {
                    val job = scope.launch {
                        withContext(Dispatchers.IO) {
                            modelFile.dir.mkdirs()

                            if (localFile.exists()) {
                                localFile.delete()
                            }
                        }
                        val downloadState =
                            downloadId?.let { getDownloadState(downloadManager, it) }
                        if (downloadState == null) {
                            downloadId = null
                        }
                        val downloadId = downloadId ?: downloadManager.enqueue(
                            DownloadManager.Request(model.url.toUri())
                                .setDestination(context, modelFile)
                                .setTitle(model.name)
                                .setDescription("Downloading ${model.name}")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        ).also {
                            downloadId = it
                        }

                        do {
                            val downloadState = getDownloadState(downloadManager, downloadId)
                            dispatch(
                                state().copy(
                                    state = downloadState.toLlmState()
                                )
                            )
                            delay(300)
                        } while (downloadState is DownloadState.Downloading)
                    }
                    dispatch(
                        state.copy(
                            downloadJob = job,
                        )
                    )
                }
            }
        }
    }
}

private fun getDownloadState(downloadManager: DownloadManager, downloadId: Long): DownloadState? {
    val query = DownloadManager.Query().setFilterById(downloadId)
    return downloadManager.query(query).use { cursor ->
        if (!cursor.moveToFirst()) {
            return@use null
        }
        val status = cursor.getInt(DownloadManager.COLUMN_STATUS)
        val reason = cursor.getInt(DownloadManager.COLUMN_REASON).asReasonString()
        val bytesDownloaded = cursor.getInt(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val bytesTotal = cursor.getInt(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        when (status) {
            DownloadManager.STATUS_FAILED -> DownloadState.Error("Failed: $reason")
            DownloadManager.STATUS_PAUSED -> DownloadState.Error("Paused: $reason")
            DownloadManager.STATUS_PENDING -> DownloadState.Downloading(
                downloadId = downloadId,
                progress = 0f,
            )

            DownloadManager.STATUS_RUNNING -> DownloadState.Downloading(
                downloadId = downloadId,
                progress = bytesDownloaded.toFloat() / bytesTotal,
            )

            DownloadManager.STATUS_SUCCESSFUL -> DownloadState.Downloaded(
                file = File(""),
            )

            else -> DownloadState.Error("Unknown: $status")
        }
    }
}

private fun Int.asStatusString(): String {
    return when (this) {
        DownloadManager.STATUS_FAILED -> "Failed"
        DownloadManager.STATUS_PAUSED -> "Paused"
        DownloadManager.STATUS_PENDING -> "Pending"
        DownloadManager.STATUS_RUNNING -> "Running"
        DownloadManager.STATUS_SUCCESSFUL -> "Successful"
        else -> "Unknown: $this"
    }
}

private fun Int.asReasonString(): String {
    return when (this) {
        DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
        DownloadManager.ERROR_FILE_ERROR -> "File error"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
        DownloadManager.ERROR_UNKNOWN -> "Unknown"
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "Paused waiting to retry"
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Paused waiting for network"
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Paused queued for WiFi"
        DownloadManager.PAUSED_UNKNOWN -> "Paused unknown"
        else -> "Unknown: $this"
    }
}

private fun DownloadState?.toLlmState(): LlmModel.State {
    return when (this) {
        is DownloadState.Downloading -> LlmModel.State.Downloading(
            downloadId = downloadId,
            progress = progress,
        )

        is DownloadState.Downloaded -> LlmModel.State.Downloaded(
            file = file,
        )

        is DownloadState.Error -> LlmModel.State.NotDownloaded(
            error = "Download error: $message",
        )

        null -> LlmModel.State.NotDownloaded(null)
    }
}

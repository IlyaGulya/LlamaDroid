package me.gulya.llamadroid.download

import java.io.File

@JvmInline
value class LlmModelId(val value: String)

data class LlmModel(
    val id: LlmModelId,
    val name: String,
    val filename: String,
    val url: String,
    val sha256: String,
) {
    sealed interface State {
        data object Initializing : State
        data class NotDownloaded(
            val error: String?,
        ) : State

        data class Downloading(
            val downloadId: Long,
            val progress: Float,
        ) : State

        data class Downloaded(
            val file: File,
        ) : State
    }
}
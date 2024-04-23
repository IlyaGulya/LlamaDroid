package me.gulya.llamadroid.download

import android.app.DownloadManager.Request
import android.content.Context
import androidx.core.net.toUri
import java.io.File

sealed interface ModelFile {
    val dir: File
    val filename: String

    val file: File
        get() = dir.resolve(filename)

    data class InternalFilesDir(
        override val dir: File,
        override val filename: String,
    ) : ModelFile

    data class ExternalFilesDir(
        override val dir: File,
        val dirType: String,
        override val filename: String,
    ) : ModelFile
}

fun Request.setDestination(context: Context, modelFile: ModelFile): Request {
    return when (modelFile) {
        is ModelFile.InternalFilesDir -> {
            setDestinationUri(modelFile.file.toUri())
        }

        is ModelFile.ExternalFilesDir -> {
            setDestinationInExternalFilesDir(
                context,
                modelFile.dirType,
                modelFile.filename,
            )
        }
    }
}

sealed interface DownloadState {
    data class Downloading(val downloadId: Long, val progress: Float) : DownloadState
    data class Downloaded(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}
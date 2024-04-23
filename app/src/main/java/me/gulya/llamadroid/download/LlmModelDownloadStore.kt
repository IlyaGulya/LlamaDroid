package me.gulya.llamadroid.download

import android.database.Cursor
import com.arkivanov.mvikotlin.core.store.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import me.gulya.llamadroid.download.LlmModelDownloadStore.Intent
import me.gulya.llamadroid.download.LlmModelDownloadStore.Label
import me.gulya.llamadroid.download.LlmModelDownloadStore.State
import java.io.File
import java.security.MessageDigest

interface LlmModelDownloadStore : Store<Intent, State, Label> {

    sealed interface Intent {
        object Download : Intent
    }

    data class State(
        val model: LlmModel,
        val downloadJob: Job?,
        val state: LlmModel.State
    )

    sealed interface Label {
    }
}

fun Cursor.getInt(columnName: String): Int {
    return getInt(getColumnIndex(columnName))
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun sha256(localFile: File): String {
    return withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        localFile.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            do {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                md.update(buffer, 0, bytesRead)
            } while (true)
        }
        md.digest().toHexString()
    }
}
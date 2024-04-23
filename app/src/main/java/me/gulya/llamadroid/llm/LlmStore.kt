package me.gulya.llamadroid.llm

import com.arkivanov.mvikotlin.core.store.Store
import kotlinx.collections.immutable.PersistentList
import me.gulya.llamadroid.download.LlmModel
import me.gulya.llamadroid.llm.LlmStore.Intent
import me.gulya.llamadroid.llm.LlmStore.Label
import me.gulya.llamadroid.llm.LlmStore.State
import java.io.File

interface LlmStore : Store<Intent, State, Label> {

    sealed interface Intent {
        class Load(
            val llmModel: LlmModel,
            val modelFile: File,
        ) : Intent

        class SendMessage(val message: String) : Intent
    }

    data class State(
        val logs: PersistentList<String>,
        val modelState: ModelState,
    )

    sealed interface ModelState {
        data object Idle : ModelState
        data class Initializing(
            val llmModel: LlmModel,
            val modelFile: File,
        ) : ModelState

        data class Ready(
            val llmModel: LlmModel,
        ) : ModelState

        data class ShouldRespond(
            val llmModel: LlmModel,
            val message: String,
        ) : ModelState

        data class Responding(
            val llmModel: LlmModel,
            val response: String,
        ) : ModelState

        data class Error(
            val llmModel: LlmModel,
            val error: Throwable,
        ) : ModelState
    }

    sealed interface Label {
    }
}


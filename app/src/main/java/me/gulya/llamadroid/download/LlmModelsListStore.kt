package me.gulya.llamadroid.download

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import me.gulya.llamadroid.download.LlmModelsListStore.Intent
import me.gulya.llamadroid.download.LlmModelsListStore.Label
import me.gulya.llamadroid.download.LlmModelsListStore.State

interface LlmModelsListStore : Store<Intent, State, Label> {

    sealed interface Intent {
    }

    data class State(
        val models: List<LlmModel>
    )

    sealed interface Label {
    }
}

object PassthroughReducer : Reducer<Any, Any> {
    override fun Any.reduce(message: Any): Any = message
}

fun <State : Any> passthroughReducer() = PassthroughReducer as Reducer<State, State>
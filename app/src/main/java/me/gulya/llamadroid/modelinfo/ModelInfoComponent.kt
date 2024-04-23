package me.gulya.llamadroid.modelinfo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.flow.map
import me.gulya.llamadroid.download.LlmModelDownloadStore

class ModelInfoComponent(
    componentContext: ComponentContext,
    llmModelDownloadStore: LlmModelDownloadStore,
) : ComponentContext by componentContext {

    val state = llmModelDownloadStore.stateFlow.map { state ->
        State(
            model = state.model,
            downloadId = state.downloadId,
            downloadState = state.downloadState,
        )
    }

}
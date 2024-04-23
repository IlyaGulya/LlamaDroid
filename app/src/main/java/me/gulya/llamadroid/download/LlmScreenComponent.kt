package me.gulya.llamadroid.download

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import me.gulya.llamadroid.llm.LlmStore
import me.gulya.llamadroid.llm.LlmStoreFactory

class LlmScreenComponent(
    private val componentContext: ComponentContext,
    private val llmStoreFactory: LlmStoreFactory,
    private val llmModelsListStoreFactory: LlmModelsListStoreFactory,
    private val llmModelDownloadStore: LlmModelDownloadStoreFactory,
) : ComponentContext by componentContext {

    private val llmModelsListStore = instanceKeeper.getStore {
        llmModelsListStoreFactory.create(defaultModels)
    }

    private val llmStore = instanceKeeper.getStore {
        llmStoreFactory.create()
    }

    private val llmDownloadStores = mutableMapOf<LlmModelId, LlmModelDownloadStore>()
    private val llmModels = llmModelsListStore
        .stateFlow
        .flatMapMerge {
            val flows = it.models.map { model ->
                val downloadStore = llmDownloadStores.getOrPut(model.id) {
                    llmModelDownloadStore.create(model)
                }
                downloadStore
                    .stateFlow
                    .map { downloadState ->
                        LlmModelVm(
                            model = model,
                            state = downloadState.state,
                            onClick = {
                                when (val downloadState = downloadStore.state.state) {
                                    is LlmModel.State.Downloaded -> {
                                        llmStore.accept(
                                            LlmStore.Intent.Load(
                                                llmModel = model,
                                                modelFile = downloadState.file,
                                            )
                                        )
                                    }

                                    is LlmModel.State.Downloading -> {
                                        downloadStore.accept(LlmModelDownloadStore.Intent.Download)
                                    }

                                    LlmModel.State.Initializing -> Unit
                                    is LlmModel.State.NotDownloaded -> {
                                        downloadStore.accept(LlmModelDownloadStore.Intent.Download)
                                    }
                                }
                            }
                        )
                    }
            }
            combine(flows) { models ->
                models.toList()
            }
        }

    val state = llmModels.combine(llmStore.stateFlow) { models, llmState ->
        LlmScreenState(
            currentState = llmState.modelState,
            modelVms = models,
            onSendMessageClick = { llmStore.accept(LlmStore.Intent.SendMessage(it)) }
        )
    }
}

data class LlmModelVm(
    val model: LlmModel,
    val state: LlmModel.State,
    val onClick: () -> Unit,
)

data class LlmScreenState(
    val currentState: LlmStore.ModelState,
    val modelVms: List<LlmModelVm>,
    val onSendMessageClick: (message: String) -> Unit,
)

val defaultModels = listOf(
    LlmModel(
        id = LlmModelId("Phi-2 7B (Q4_0, 1.6 GiB)-1"),
        name = "Phi-2 7B (Q4_0, 1.6 GiB)",
        filename = "phi-2-q4_0.gguf",
        url = "https://huggingface.co/ggml-org/models/resolve/main/phi-2/ggml-model-q4_0.gguf?download=true",
        sha256 = "fd506d24a4bee6997a566b02b65715af5cadb433c6a3a47a74b467badc5727ca",
    ),
    LlmModel(
        id = LlmModelId("TinyLlama 1.1B (f16, 2.2 GiB)-1"),
        name = "TinyLlama 1.1B (f16, 2.2 GiB)",
        filename = "tinyllama-1.1-f16.gguf",
        url = "https://huggingface.co/ggml-org/models/resolve/main/tinyllama-1.1b/ggml-model-f16.gguf?download=true",
        sha256 = "92982a0b96adfe5a8cea15ed6272bd11282f9a257eca74e40225becc6ae61c71",
    ),
    LlmModel(
        id = LlmModelId("Phi 2 DPO (Q3_K_M, 1.48 GiB)-1"),
        name = "Phi 2 DPO (Q3_K_M, 1.48 GiB)",
        filename = "phi-2-dpo.Q3_K_M.gguf",
        url = "https://huggingface.co/TheBloke/phi-2-dpo-GGUF/resolve/main/phi-2-dpo.Q3_K_M.gguf?download=true",
        sha256 = "e7effd3e3a3b6f1c05b914deca7c9646210bad34576d39d3c5c5f2a25cb97ae1",
    ),
)

package me.gulya.llamadroid.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import me.gulya.llamadroid.download.LlmModel
import me.gulya.llamadroid.download.LlmModelDownloadStore
import me.gulya.llamadroid.download.LlmModelDownloadStoreFactory
import me.gulya.llamadroid.download.LlmModelId
import me.gulya.llamadroid.download.LlmModelVm
import me.gulya.llamadroid.download.LlmModelsListStoreFactory
import me.gulya.llamadroid.download.defaultModels
import me.gulya.llamadroid.llm.LlmStoreFactory
import me.gulya.llamadroid.root.RootChild

fun interface OnLlmClick {
    fun invoke(model: LlmModel)
}

fun interface NewChatComponentFactory {
    fun create(componentContext: ComponentContext, onLlmClick: OnLlmClick): NewChatComponent
}

class NewChatComponent(
    componentContext: ComponentContext,
    private val onLlmClick: OnLlmClick,
    private val llmStoreFactory: LlmStoreFactory,
    private val llmModelDownloadStoreFactory: LlmModelDownloadStoreFactory,
    private val llmModelsListStoreFactory: LlmModelsListStoreFactory,
) : ComponentContext by componentContext, RootChild {

    val llmModelsListStore = instanceKeeper.getStore {
        llmModelsListStoreFactory.create(defaultModels)
    }

    val llmStore = instanceKeeper.getStore {
        llmStoreFactory.create()
    }

    private val llmDownloadStores = mutableMapOf<LlmModelId, LlmModelDownloadStore>()
    private val llmModels = llmModelsListStore.stateFlow.flatMapMerge {
        val flows = it.models.map { model ->
            val downloadStore = llmDownloadStores.getOrPut(model.id) {
                llmModelDownloadStoreFactory.create(model)
            }
            downloadStore
                .stateFlow
                .map { downloadState ->
                    LlmModelVm(
                        model = model,
                        state = downloadState.state,
                        onClick = {
                            onLlmClick(model)
//                            when (val downloadState = downloadStore.state.state) {
//                                is LlmModel.State.Downloaded -> {
//                                    llmStore.accept(
//                                        LlmStore.Intent.Load(
//                                            llmModel = model,
//                                            modelFile = downloadState.file,
//                                        )
//                                    )
//                                }
//
//                                is LlmModel.State.Downloading -> {
//                                    downloadStore.accept(LlmModelDownloadStore.Intent.Download)
//                                }
//
//                                LlmModel.State.Initializing -> Unit
//                                is LlmModel.State.NotDownloaded -> {
//                                    downloadStore.accept(LlmModelDownloadStore.Intent.Download)
//                                }
//                            }
                        }
                    )
                }
        }
        combine(flows) { models ->
            models.toList()
        }
    }

    val state = llmModels.map { models ->
        val models = models.map { model ->
            ModelVm(
                name = model.model.name,
                state = when (model.state) {
                    is LlmModel.State.Initializing -> ModelVm.State.NotDownloaded
                    is LlmModel.State.Downloaded -> ModelVm.State.Downloaded
                    is LlmModel.State.Downloading -> ModelVm.State.Downloading(model.state.progress)
                    is LlmModel.State.NotDownloaded -> ModelVm.State.NotDownloaded
                },
                onClick = model.onClick
            )
        }
        NewChatVm(
            localModels = models.filter { it.state !is ModelVm.State.NotDownloaded },
            remoteModels = models.filter { it.state is ModelVm.State.NotDownloaded },
        )
    }
}

data class NewChatVm(
    val localModels: List<ModelVm>,
    val remoteModels: List<ModelVm>,
)

data class ModelVm(
    val name: String,
    val state: State,
    val onClick: () -> Unit,
) {

    sealed interface State {
        data object Downloaded : State
        data class Downloading(
            val progress: Float,
        ) : State

        data object NotDownloaded : State
    }
}


@Composable
fun NewChatContent(
    component: NewChatComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState(initial = NewChatVm(emptyList(), emptyList()))
    NewChatContent(
        vm = state,
        modifier = modifier
    )
}

@Composable
fun NewChatContent(
    vm: NewChatVm,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item("header") {
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Please choose\n" +
                            "a model",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "To be able to chat with a \n" +
                            "Large Language Model (LLM) you\n" +
                            "need to choose and download one",
                    style = MaterialTheme.typography.bodyMedium,
                )

                HorizontalLine()

                Text(
                    text = "Available locally",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        items(vm.localModels) {
            val color = when (it.state) {
                ModelVm.State.Downloaded -> Color.Green
                is ModelVm.State.Downloading -> Color.Green
                ModelVm.State.NotDownloaded -> Color.Red
            }
            val progress = if (it.state is ModelVm.State.Downloading) {
                it.state.progress
            } else {
                1f
            }
            ButtonWithProgress(
                modifier = Modifier.fillMaxWidth(0.9f),
                text = it.name,
                progress = progress,
                enabled = true,
                color = color,
                onClick = it.onClick,
            )
        }

        item("header_remote") {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Available for download",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        items(vm.remoteModels) {
            val color = when (it.state) {
                ModelVm.State.Downloaded -> Color.Green
                is ModelVm.State.Downloading -> Color.Green
                ModelVm.State.NotDownloaded -> Color.Red
            }
            ButtonWithProgress(
                modifier = Modifier.fillMaxWidth(0.9f),
                text = it.name,
                progress = 1f,
                enabled = true,
                color = color,
                onClick = it.onClick,
            )
        }
    }
}

@Composable
fun ButtonWithProgress(
    text: String,
    progress: Float,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .drawWithContent {
                val progressWidth = size.copy(width = size.width * progress)

                drawOutline(
                    shape.createOutline(
                        progressWidth,
                        layoutDirection,
                        this
                    ),
                    color
                )

                drawContent()
            },
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color.Red, Color.Blue))),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun HorizontalLine(modifier: Modifier = Modifier) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onPrimary)
    )
}

@Preview
@Composable
private fun PreviewNewChatScreen() {
    NewChatContent(
        vm = NewChatVm(
            localModels = listOf(
                ModelVm(
                    name = "TinyLlama",
                    state = ModelVm.State.Downloading(0.7f),
                    onClick = {},
                )
            ),
            remoteModels = listOf(
                ModelVm(
                    name = "Phi-2 7B (Q4_0, 1.6 GiB)",
                    state = ModelVm.State.NotDownloaded,
                    onClick = {},
                )
            )
        )
    )
}
package me.gulya.llamadroid.llm

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.gulya.llamadroid.download.passthroughReducer
import me.gulya.llamadroid.llama.GgmlLogLevel
import me.gulya.llamadroid.llama.Llm
import me.gulya.llamadroid.llm.LlmStore.Intent
import me.gulya.llamadroid.llm.LlmStore.Label
import me.gulya.llamadroid.llm.LlmStore.ModelState
import me.gulya.llamadroid.llm.LlmStore.State

class LlmStoreFactory(
    private val storeFactory: StoreFactory
) {

    fun create(): LlmStore =
        object : LlmStore, Store<Intent, State, Label> by storeFactory.create(
            name = "LlmStore",
            initialState = State(persistentListOf(), ModelState.Idle),
            bootstrapper = Bootstrapper(),
            executorFactory = ::ExecutorImpl,
            reducer = passthroughReducer()
        ) {}

    private sealed interface Action {
        data object NextStep : Action
        data class Log(
            @GgmlLogLevel
            val level: Int,
            val message: String
        ) : Action
    }

    private class Bootstrapper : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.launch {
                Llm.setLogger { priority, message ->
                    dispatch(Action.Log(priority, message))
                }
            }
        }

    }

    private class ExecutorImpl : CoroutineExecutor<Intent, Action, State, State, Label>() {
        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.Load -> {
                    dispatch(
                        ModelState.Initializing(
                            intent.llmModel,
                            intent.modelFile,
                        )
                    )
                    executeAction(Action.NextStep)
                }

                is Intent.SendMessage -> {
                    val state = modelState()
                    if (state !is ModelState.Ready) return
                    dispatch(
                        ModelState.ShouldRespond(
                            llmModel = state.llmModel,
                            message = intent.message
                        )
                    )
                    executeAction(Action.NextStep)
                }
            }
        }

        override fun executeAction(action: Action) {
            when (action) {
                is Action.Log -> {
                    dispatch(
                        state().copy(
                            logs = state().logs.add(action.message)
                        )
                    )
                }

                is Action.NextStep -> Unit
            }
            if (action !is Action.NextStep) return

            when (val state = modelState()) {
                is ModelState.Error -> TODO()
                ModelState.Idle -> Unit
                is ModelState.Initializing -> {
                    scope.launch {
                        Llm.load(state.modelFile.absolutePath)
                        dispatch(ModelState.Ready(state.llmModel))
                    }
                }

                is ModelState.ShouldRespond -> {
                    scope.launch {
                        dispatch(
                            ModelState.Responding(
                                llmModel = state.llmModel,
                                response = ""
                            )
                        )
                        Llm.send(state.message).collectLatest {
                            val state = modelOfState<ModelState.Responding>() ?: return@collectLatest
                            dispatch(
                                ModelState.Responding(
                                    llmModel = state.llmModel,
                                    response = it,
                                )
                            )
                        }
                    }
                }

                is ModelState.Responding -> Unit
                is ModelState.Ready -> Unit
            }
        }

        private fun modelState(): ModelState = state().modelState
        private fun <T : ModelState> modelOfState(): T? = state().modelState as? T

        private fun dispatch(modelState: ModelState) {
            dispatch(
                state().copy(
                    modelState = modelState
                )
            )
        }
    }

}
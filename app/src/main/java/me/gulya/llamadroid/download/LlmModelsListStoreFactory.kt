package me.gulya.llamadroid.download

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor

class LlmModelsListStoreFactory(
    private val storeFactory: StoreFactory
) {

    fun create(models: List<LlmModel>): LlmModelsListStore =
        object : LlmModelsListStore,
            Store<LlmModelsListStore.Intent, LlmModelsListStore.State, LlmModelsListStore.Label> by storeFactory.create(
                name = "DownloadsStore",
                initialState = LlmModelsListStore.State(models),
                bootstrapper = BootstrapperImpl(),
                executorFactory = ::ExecutorImpl,
                reducer = passthroughReducer(),
            ) {}

    private sealed interface Action {
    }

    private class BootstrapperImpl : CoroutineBootstrapper<Action>() {
        override fun invoke() {
        }
    }

    private class ExecutorImpl :
        CoroutineExecutor<LlmModelsListStore.Intent, Action, LlmModelsListStore.State, LlmModelsListStore.State, LlmModelsListStore.Label>() {
        override fun executeIntent(intent: LlmModelsListStore.Intent) {
        }

        override fun executeAction(action: Action) {
        }
    }
}
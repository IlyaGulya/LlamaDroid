package me.gulya.llamadroid

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.mvikotlin.logging.store.LoggingStoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import me.gulya.llamadroid.download.LlmModelDownloadStoreFactory
import me.gulya.llamadroid.download.LlmModelsListStoreFactory
import me.gulya.llamadroid.llm.LlmStoreFactory
import me.gulya.llamadroid.newchat.NewChatComponent
import me.gulya.llamadroid.newchat.NewChatContent
import me.gulya.llamadroid.root.RootComponent
import me.gulya.llamadroid.root.Screen
import me.gulya.llamadroid.ui.theme.LlamaAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        val storeFactory = LoggingStoreFactory(DefaultStoreFactory())

        val llmStoreFactory = LlmStoreFactory(storeFactory)
        val llmModelsListStoreFactory = LlmModelsListStoreFactory(storeFactory)
        val llmModelDownloadStoreFactory = LlmModelDownloadStoreFactory(
            storeFactory = storeFactory,
            context = this.applicationContext
        )

        val rootComponent = RootComponent(
            componentContext = defaultComponentContext(),
            chatComponentFactory = { componentContext ->
                NewChatComponent(
                    componentContext = componentContext,
                    onLlmClick = { model ->
                        navigation.push(Screen.ModelInfo(model))
                    },
                    llmStoreFactory = llmStoreFactory,
                    llmModelsListStoreFactory = llmModelsListStoreFactory,
                    llmModelDownloadStoreFactory = llmModelDownloadStoreFactory,
                )
            },
        )

        setContent {
            LlamaAndroidTheme {
                Children(stack = rootComponent.childStack) { child ->
                    when (val component = child.instance) {
                        is NewChatComponent -> NewChatContent(component = component)
                    }
                }
            }
        }
    }
}
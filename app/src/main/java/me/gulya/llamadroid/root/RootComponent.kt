package me.gulya.llamadroid.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import me.gulya.llamadroid.download.LlmModel
import me.gulya.llamadroid.newchat.NewChatComponentFactory

class RootComponent(
    componentContext: ComponentContext,
    chatComponentFactory: NewChatComponentFactory,
) : ComponentContext by componentContext {

    val navigation = StackNavigation<Screen>()

    val childStack: Value<ChildStack<Screen, RootChild>> = childStack(
        initialConfiguration = Screen.NewChat,
        key = "RootComponent",
        serializer = Screen.serializer(),
        source = navigation,
        childFactory = { configuration, componentContext ->
            when (configuration) {
                is Screen.NewChat -> chatComponentFactory.create(
                    componentContext = componentContext,
                    onLlmClick = {
                        navigation.push(Screen.ModelInfo(it))
                    }
                )
                is Screen.ModelInfo ->
            }
        }
    )

}

interface RootChild

@Serializable
sealed interface Screen {
    data object NewChat : Screen
    data class ModelInfo(
        val model: LlmModel,
    ) : Screen
}
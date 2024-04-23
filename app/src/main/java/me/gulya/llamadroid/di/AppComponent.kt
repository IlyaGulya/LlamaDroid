package me.gulya.llamadroid.di

import com.squareup.anvil.annotations.MergeComponent
import javax.inject.Singleton

@Singleton
@MergeComponent(AppScope::class)
interface AppComponent

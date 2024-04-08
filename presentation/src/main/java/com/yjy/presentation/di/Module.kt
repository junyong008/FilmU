package com.yjy.presentation.di

import android.content.Context
import com.yjy.presentation.util.DisplayManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Module {

    @Singleton
    @Provides
    fun provideDisplayManager(@ApplicationContext context: Context): DisplayManager {
        return DisplayManager(context)
    }
}
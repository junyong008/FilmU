package com.yjy.data.di

import android.content.Context
import com.yjy.data.repository.media.local.MediaLocalDataSource
import com.yjy.data.repository.media.local.MediaLocalDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class LocalModule {

    @Provides
    @ViewModelScoped
    fun provideMediaLocalDataSource(@ApplicationContext context: Context): MediaLocalDataSource {
        return MediaLocalDataSourceImpl(context)
    }
}
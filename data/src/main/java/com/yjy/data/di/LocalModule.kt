package com.yjy.data.di

import android.content.Context
import com.yjy.data.repository.media.local.MediaLocalDataSource
import com.yjy.data.repository.media.local.MediaLocalDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LocalModule {

    @Provides
    @Singleton
    fun provideMediaLocalDataSource(@ApplicationContext context: Context): MediaLocalDataSource {
        return MediaLocalDataSourceImpl(context)
    }
}
package com.yjy.data.di

import com.yjy.data.repository.media.MediaRepositoryImpl
import com.yjy.data.repository.media.local.MediaLocalDataSource
import com.yjy.domain.repository.MediaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideMediaRepository(mediaLocalDataSource: MediaLocalDataSource): MediaRepository {
        return MediaRepositoryImpl(mediaLocalDataSource)
    }
}
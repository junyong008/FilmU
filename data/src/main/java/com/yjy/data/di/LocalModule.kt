package com.yjy.data.di

import android.content.Context
import com.yjy.data.local.database.ExampleDatabase
import com.yjy.data.local.database.dao.GithubRepoDao
import com.yjy.data.local.datastore.ExampleDataStore
import com.yjy.data.repository.githubrepo.local.GithubRepoLocalDataSource
import com.yjy.data.repository.githubrepo.local.GithubRepoLocalDataSourceImpl
import com.yjy.data.repository.number.local.NumberLocalDataSource
import com.yjy.data.repository.number.local.NumberLocalDataSourceImpl
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
    fun provideNumberLocalDataSource(exampleDataStore: ExampleDataStore): NumberLocalDataSource {
        return NumberLocalDataSourceImpl(exampleDataStore)
    }

    @Provides
    @Singleton
    fun provideExampleDataStore(@ApplicationContext context: Context): ExampleDataStore {
        return ExampleDataStore(context)
    }

    @Provides
    @Singleton
    fun provideGithubRepoLocalDataSource(githubRepoDao: GithubRepoDao): GithubRepoLocalDataSource {
        return GithubRepoLocalDataSourceImpl(githubRepoDao)
    }

    @Singleton
    @Provides
    fun provideGithubRepoDao(exampleDatabase: ExampleDatabase): GithubRepoDao {
        return exampleDatabase.githubRepoDao()
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): ExampleDatabase {
        return ExampleDatabase.getInstance(context)
    }
}
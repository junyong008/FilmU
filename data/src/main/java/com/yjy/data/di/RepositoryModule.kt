package com.yjy.data.di

import com.yjy.data.repository.githubrepo.GithubRepoRepositoryImpl
import com.yjy.data.repository.githubrepo.local.GithubRepoLocalDataSource
import com.yjy.data.repository.githubrepo.remote.GithubRepoRemoteDataSource
import com.yjy.data.repository.joke.JokeRepositoryImpl
import com.yjy.data.repository.joke.remote.JokeRemoteDataSource
import com.yjy.data.repository.number.NumberRepositoryImpl
import com.yjy.data.repository.number.local.NumberLocalDataSource
import com.yjy.domain.repository.GithubRepoRepository
import com.yjy.domain.repository.JokeRepository
import com.yjy.domain.repository.NumberRepository
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
    fun provideNumberRepository(numberLocalDataSource: NumberLocalDataSource): NumberRepository {
        return NumberRepositoryImpl(numberLocalDataSource)
    }

    @Provides
    @Singleton
    fun provideJokeRepository(jokeRemoteDataSource: JokeRemoteDataSource): JokeRepository {
        return JokeRepositoryImpl(jokeRemoteDataSource)
    }

    @Provides
    @Singleton
    fun provideGithubRepoRepository(
        githubRepoLocalDataSource: GithubRepoLocalDataSource,
        githubRepoRemoteDataSource: GithubRepoRemoteDataSource,
    ): GithubRepoRepository {
        return GithubRepoRepositoryImpl(githubRepoLocalDataSource, githubRepoRemoteDataSource)
    }
}
package com.yjy.domain.di

import com.yjy.domain.repository.GithubRepoRepository
import com.yjy.domain.repository.JokeRepository
import com.yjy.domain.repository.NumberRepository
import com.yjy.domain.usecase.AddNumberUseCase
import com.yjy.domain.usecase.GetGithubReposUseCase
import com.yjy.domain.usecase.GetNumberUseCase
import com.yjy.domain.usecase.GetRandomJokeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Provides
    @Singleton
    fun provideAddNumberUseCase(numberRepository: NumberRepository): AddNumberUseCase {
        return AddNumberUseCase(numberRepository)
    }


    @Provides
    @Singleton
    fun provideGetNumberUseCase(numberRepository: NumberRepository): GetNumberUseCase {
        return GetNumberUseCase(numberRepository)
    }


    @Provides
    @Singleton
    fun provideGetGithubReposUseCase(githubRepoRepository: GithubRepoRepository): GetGithubReposUseCase {
        return GetGithubReposUseCase(githubRepoRepository)
    }

    @Provides
    @Singleton
    fun provideGetRandomJokeUseCase(jokeRepository: JokeRepository): GetRandomJokeUseCase {
        return GetRandomJokeUseCase(jokeRepository)
    }
}
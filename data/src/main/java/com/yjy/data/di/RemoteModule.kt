package com.yjy.data.di

import com.yjy.data.Const.BASE_URL_GITHUB
import com.yjy.data.Const.BASE_URL_JOKE
import com.yjy.data.remote.adapter.ResultCallAdapterFactory
import com.yjy.data.remote.service.GithubRepoService
import com.yjy.data.remote.service.JokeService
import com.yjy.data.repository.githubrepo.remote.GithubRepoRemoteDataSource
import com.yjy.data.repository.githubrepo.remote.GithubRepoRemoteDataSourceImpl
import com.yjy.data.repository.joke.remote.JokeRemoteDataSource
import com.yjy.data.repository.joke.remote.JokeRemoteDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RemoteModule {

    @Singleton
    @Provides
    fun provideJokeService(@Named("jokeRetrofit") retrofit: Retrofit): JokeService {
        return retrofit.create(JokeService::class.java)
    }

    @Provides
    @Singleton
    fun provideJokeRemoteDataSource(jokeService: JokeService): JokeRemoteDataSource {
        return JokeRemoteDataSourceImpl(jokeService)
    }

    @Provides
    @Singleton
    fun provideGithubRepoRemoteDataSource(githubRepoService: GithubRepoService): GithubRepoRemoteDataSource {
        return GithubRepoRemoteDataSourceImpl(githubRepoService)
    }

    @Singleton
    @Provides
    fun provideGithubRepoService(retrofit: Retrofit): GithubRepoService {
        return retrofit.create(GithubRepoService::class.java)
    }

    // 단일 데이터(Joke)를 서버로부터 받아오기 위한 Retrofit
    @Singleton
    @Provides
    @Named("jokeRetrofit")
    fun provideJokeRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_JOKE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }

    // Github Repository 목록을 받아오기 위한 BASE
    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_GITHUB)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
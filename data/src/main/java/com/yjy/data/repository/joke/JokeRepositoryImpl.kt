package com.yjy.data.repository.joke

import com.yjy.data.repository.joke.remote.JokeRemoteDataSource
import com.yjy.domain.model.ApiResult
import com.yjy.domain.model.Joke
import com.yjy.domain.model.map
import com.yjy.domain.repository.JokeRepository
import javax.inject.Inject

class JokeRepositoryImpl @Inject constructor(
    private val jokeRemoteDataSource: JokeRemoteDataSource
) : JokeRepository {

    override suspend fun getJoke(): ApiResult<Joke> = jokeRemoteDataSource.getJoke().map { it.toJoke() }
}
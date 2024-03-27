package com.yjy.data.repository.joke.remote

import com.yjy.data.model.dto.JokeDto
import com.yjy.data.remote.service.JokeService
import com.yjy.domain.model.ApiResult
import javax.inject.Inject

class JokeRemoteDataSourceImpl @Inject constructor(private val jokeService: JokeService) :
    JokeRemoteDataSource {

    override suspend fun getJoke(): ApiResult<JokeDto> = jokeService.getJoke()
}
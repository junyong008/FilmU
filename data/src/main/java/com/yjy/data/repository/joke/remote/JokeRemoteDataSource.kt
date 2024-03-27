package com.yjy.data.repository.joke.remote

import com.yjy.data.model.dto.JokeDto
import com.yjy.domain.model.ApiResult

interface JokeRemoteDataSource {
    suspend fun getJoke(): ApiResult<JokeDto>
}
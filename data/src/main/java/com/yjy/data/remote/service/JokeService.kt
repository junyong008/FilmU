package com.yjy.data.remote.service

import com.yjy.data.model.dto.JokeDto
import com.yjy.domain.model.ApiResult
import retrofit2.http.*

interface JokeService {
    @GET("jokes/random")
    suspend fun getJoke(): ApiResult<JokeDto>
}
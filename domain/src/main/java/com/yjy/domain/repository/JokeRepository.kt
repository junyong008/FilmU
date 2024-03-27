package com.yjy.domain.repository

import com.yjy.domain.model.ApiResult
import com.yjy.domain.model.Joke

interface JokeRepository {
    suspend fun getJoke(): ApiResult<Joke>
}
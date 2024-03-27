package com.yjy.domain.usecase

import com.yjy.domain.model.ApiResult
import com.yjy.domain.model.Joke
import com.yjy.domain.repository.JokeRepository
import javax.inject.Inject

class GetRandomJokeUseCase @Inject constructor(private val jokeRepository: JokeRepository) {
    suspend operator fun invoke(): ApiResult<Joke> = jokeRepository.getJoke()
}
package com.yjy.domain.usecase

import com.yjy.domain.repository.NumberRepository
import javax.inject.Inject

class AddNumberUseCase @Inject constructor(private val numberRepository: NumberRepository) {
    suspend operator fun invoke(number: Int) = numberRepository.addNumber(number)
}
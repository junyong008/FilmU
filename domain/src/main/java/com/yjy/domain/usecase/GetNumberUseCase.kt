package com.yjy.domain.usecase

import com.yjy.domain.repository.NumberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNumberUseCase @Inject constructor(private val numberRepository: NumberRepository) {
    operator fun invoke(): Flow<Int> = numberRepository.getNumber()
}
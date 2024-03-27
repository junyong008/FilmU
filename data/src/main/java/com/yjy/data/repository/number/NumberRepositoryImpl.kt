package com.yjy.data.repository.number

import com.yjy.data.repository.number.local.NumberLocalDataSource
import com.yjy.domain.repository.NumberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NumberRepositoryImpl @Inject constructor(
    private val numberLocalDataSource: NumberLocalDataSource
) : NumberRepository {
    override fun getNumber(): Flow<Int> = numberLocalDataSource.getNumber()
    override suspend fun addNumber(number: Int) = numberLocalDataSource.addNumber(number)
}
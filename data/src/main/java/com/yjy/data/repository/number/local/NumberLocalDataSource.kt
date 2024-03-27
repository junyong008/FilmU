package com.yjy.data.repository.number.local

import kotlinx.coroutines.flow.Flow

interface NumberLocalDataSource {
    fun getNumber(): Flow<Int>
    suspend fun addNumber(number: Int)
}
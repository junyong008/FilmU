package com.yjy.domain.repository

import kotlinx.coroutines.flow.Flow

interface NumberRepository {
    fun getNumber(): Flow<Int>
    suspend fun addNumber(number: Int)
}
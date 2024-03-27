package com.yjy.data.repository.number.local

import com.yjy.data.local.datastore.ExampleDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NumberLocalDataSourceImpl @Inject constructor(private val exampleDataStore: ExampleDataStore) :
    NumberLocalDataSource {

    override fun getNumber(): Flow<Int> = exampleDataStore.getNumber()
    override suspend fun addNumber(number: Int) = exampleDataStore.addNumber(number)
}
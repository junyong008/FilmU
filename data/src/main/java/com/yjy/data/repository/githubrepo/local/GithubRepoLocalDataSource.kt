package com.yjy.data.repository.githubrepo.local

import androidx.paging.PagingSource
import com.yjy.data.model.entity.GithubRepoEntity

interface GithubRepoLocalDataSource {
    suspend fun insertAllExamples(examples: List<GithubRepoEntity>)
    suspend fun deleteAllExamples()
    fun getPagingSource(): PagingSource<Int, GithubRepoEntity>
}
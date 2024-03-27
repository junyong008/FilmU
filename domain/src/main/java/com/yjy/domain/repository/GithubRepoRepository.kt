package com.yjy.domain.repository

import androidx.paging.PagingData
import com.yjy.domain.model.GithubRepo
import kotlinx.coroutines.flow.Flow

interface GithubRepoRepository {
    fun getGithubRepos(ownerName: String): Flow<PagingData<GithubRepo>>
}
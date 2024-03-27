package com.yjy.data.repository.githubrepo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.yjy.data.repository.githubrepo.local.GithubRepoLocalDataSource
import com.yjy.data.repository.githubrepo.remote.GithubRepoRemoteDataSource
import com.yjy.domain.model.GithubRepo
import com.yjy.domain.repository.GithubRepoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GithubRepoRepositoryImpl @Inject constructor(
    private val githubRepoLocalDataSource: GithubRepoLocalDataSource,
    private val githubRepoRemoteDataSource: GithubRepoRemoteDataSource,
) : GithubRepoRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun getGithubRepos(ownerName: String): Flow<PagingData<GithubRepo>> {
        return Pager(
            config = PagingConfig(
                pageSize = 15,
                enablePlaceholders = true
            ),
            remoteMediator = GithubRepoRemoteMediator(ownerName, githubRepoLocalDataSource, githubRepoRemoteDataSource)
        ) {
            githubRepoLocalDataSource.getPagingSource()
        }.flow.map { pagingData -> pagingData.map { it.toGithubRepo() } }
    }
}
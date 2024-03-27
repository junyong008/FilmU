package com.yjy.data.repository.githubrepo

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.yjy.data.model.entity.GithubRepoEntity
import com.yjy.data.repository.githubrepo.local.GithubRepoLocalDataSource
import com.yjy.data.repository.githubrepo.remote.GithubRepoRemoteDataSource
import com.yjy.domain.model.ApiResult
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class GithubRepoRemoteMediator @Inject constructor(
    private val ownerName: String,
    private val githubRepoLocalDataSource: GithubRepoLocalDataSource,
    private val githubRepoRemoteDataSource: GithubRepoRemoteDataSource,
) : RemoteMediator<Int, GithubRepoEntity>() {

    // 최초 실행시 REFRESH 하여 최신화된 데이터를 보여줄수 있도록 설정. SKIP_INITIAL_REFRESH 설정시 기존 캐싱 데이터 보여줌
    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GithubRepoEntity>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                if (state.lastItemOrNull() == null) {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                (state.pages.size) + 1
            }
        }

        return when(val apiResult = githubRepoRemoteDataSource.getGithubRepos(ownerName, page, state.config.pageSize)) {
            is ApiResult.Success -> {
                val repos = apiResult.data.map { it.toGithubRepoEntity() }

                // Transaction 접근 : 데이터의 원자성을 보장. 하나라도 실패하면 데이터 원복.
                githubRepoLocalDataSource.run {
                    // REFRESH 라면 기존 캐싱된 DB를 모두 지우고 데이터 추가
                    if (loadType == LoadType.REFRESH) githubRepoLocalDataSource.deleteAllExamples()
                    githubRepoLocalDataSource.insertAllExamples(repos)
                }
                MediatorResult.Success(endOfPaginationReached = repos.isEmpty())
            }
            is ApiResult.Failure -> {
                githubRepoLocalDataSource.deleteAllExamples() // 실패시 기존 캐싱된 데이터도 삭제. 개발 조건에 따라 변경
                MediatorResult.Error(apiResult.safeThrowable())
            }
        }
    }
}
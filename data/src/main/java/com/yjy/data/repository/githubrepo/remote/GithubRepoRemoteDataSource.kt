package com.yjy.data.repository.githubrepo.remote

import com.yjy.data.model.dto.GithubRepoDto
import com.yjy.domain.model.ApiResult

interface GithubRepoRemoteDataSource {
    suspend fun getGithubRepos(ownerName: String, page: Int, pageSize: Int): ApiResult<List<GithubRepoDto>>
}
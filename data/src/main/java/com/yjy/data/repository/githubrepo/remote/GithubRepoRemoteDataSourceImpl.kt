package com.yjy.data.repository.githubrepo.remote

import com.yjy.data.model.dto.GithubRepoDto
import com.yjy.data.remote.service.GithubRepoService
import com.yjy.domain.model.ApiResult
import javax.inject.Inject

class GithubRepoRemoteDataSourceImpl @Inject constructor(private val githubRepoService: GithubRepoService) :
    GithubRepoRemoteDataSource {

    override suspend fun getGithubRepos(ownerName: String, page: Int, pageSize: Int): ApiResult<List<GithubRepoDto>> {
        return githubRepoService.getGithubRepos(ownerName, page, pageSize)
    }
}
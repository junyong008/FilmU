package com.yjy.data.remote.service

import com.yjy.data.model.dto.GithubRepoDto
import com.yjy.domain.model.ApiResult
import retrofit2.http.*

interface GithubRepoService {
    @GET("users/{username}/repos")
    suspend fun getGithubRepos(
        @Path("username") username: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): ApiResult<List<GithubRepoDto>>
}
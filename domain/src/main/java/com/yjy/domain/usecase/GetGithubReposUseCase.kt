package com.yjy.domain.usecase

import androidx.paging.PagingData
import com.yjy.domain.model.GithubRepo
import com.yjy.domain.repository.GithubRepoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGithubReposUseCase @Inject constructor(private val githubRepoRepository: GithubRepoRepository) {
    operator fun invoke(ownerName: String): Flow<PagingData<GithubRepo>> = githubRepoRepository.getGithubRepos(ownerName)
}
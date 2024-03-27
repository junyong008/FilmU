package com.yjy.data.repository.githubrepo.local

import androidx.paging.PagingSource
import com.yjy.data.local.database.dao.GithubRepoDao
import com.yjy.data.model.entity.GithubRepoEntity
import javax.inject.Inject

class GithubRepoLocalDataSourceImpl @Inject constructor(private val githubRepoDao: GithubRepoDao) :
    GithubRepoLocalDataSource {

    override suspend fun insertAllExamples(examples: List<GithubRepoEntity>) =
        githubRepoDao.insertAll(examples)

    override suspend fun deleteAllExamples() = githubRepoDao.deleteAll()

    override fun getPagingSource(): PagingSource<Int, GithubRepoEntity> = githubRepoDao.getPagingSource()
}
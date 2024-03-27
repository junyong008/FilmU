package com.yjy.data.local.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yjy.data.model.entity.GithubRepoEntity

@Dao
interface GithubRepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(examples: List<GithubRepoEntity>)

    @Query("DELETE FROM github_repo")
    suspend fun deleteAll()

    @Query("SELECT * FROM github_repo")
    fun getPagingSource(): PagingSource<Int, GithubRepoEntity>
}
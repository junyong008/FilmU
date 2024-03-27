package com.yjy.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yjy.domain.model.GithubRepo

@Entity(tableName = "github_repo")
data class GithubRepoEntity(
    @PrimaryKey val id: Int,
    val ownerName: String,
    val ownerProfile: String,
    val repoName: String,
) {
    fun toGithubRepo(): GithubRepo = GithubRepo(id, ownerName, ownerProfile, repoName)
}
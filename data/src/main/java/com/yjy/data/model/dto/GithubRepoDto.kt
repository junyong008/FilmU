package com.yjy.data.model.dto

import com.google.gson.annotations.SerializedName
import com.yjy.data.model.entity.GithubRepoEntity

data class GithubRepoDto(
    val id: Int,
    val owner: OwnerDto,
    @SerializedName("name") val repoName: String,
) {
    fun toGithubRepoEntity(): GithubRepoEntity = GithubRepoEntity(id, owner.name, owner.profile, repoName)

    data class OwnerDto(
        @SerializedName("login") val name: String,
        @SerializedName("avatar_url") val profile: String,
    )
}
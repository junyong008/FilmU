package com.yjy.presentation.feature.example

import com.yjy.domain.model.GithubRepo

interface GithubRepoItemClickListener {
    fun onItemClicked(githubRepo: GithubRepo)
}
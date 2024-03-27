package com.yjy.presentation.feature.example

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.paging.LoadState
import com.yjy.domain.model.GithubRepo
import com.yjy.presentation.R
import com.yjy.presentation.base.BaseActivity
import com.yjy.presentation.databinding.ActivityExampleBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExampleActivity : BaseActivity<ActivityExampleBinding>(R.layout.activity_example) {

    private val exampleViewModel: ExampleViewModel by viewModels()
    private val githubRepoItemClickListener = object : GithubRepoItemClickListener {
        override fun onItemClicked(githubRepo: GithubRepo) {
            showToast(githubRepo.repoName)
        }
    }
    private val githubRepoAdapter = GithubRepoAdapter(githubRepoItemClickListener)

    override fun initViewModel() {
        binding.exampleViewModel = exampleViewModel
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerViewGithubRepos.adapter = githubRepoAdapter
    }

    override fun setListener() {
        githubRepoAdapter.addLoadStateListener { loadStates ->
            when(loadStates.refresh) {
                is LoadState.Loading -> {
                    binding.progressGithubRepos.isVisible = true
                    binding.buttonGetGithubRepos.isEnabled = false
                }
                is LoadState.NotLoading -> {
                    binding.progressGithubRepos.isVisible = false
                    binding.buttonGetGithubRepos.isEnabled = true
                }
                is LoadState.Error -> {
                    binding.progressGithubRepos.isVisible = false
                    binding.buttonGetGithubRepos.isEnabled = true
                    val errorState = loadStates.refresh as LoadState.Error
                    showToast(errorState.error.localizedMessage ?: "")
                }
            }
        }
    }

    override fun observeFlows() {
        collectLatestFlow(exampleViewModel.githubRepos) {
            githubRepoAdapter.submitData(it)
        }
    }

    override fun observeSharedFlow() {

        collectLatestSharedFlow(exampleViewModel.message) {
            when(it) {
                is ExampleViewModel.ExampleMessage.GetJokeFailed -> showToast(it.message ?: "Joke 받아오기 실패")
            }
        }
    }
}
package com.yjy.presentation.feature.example

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yjy.domain.model.GithubRepo
import com.yjy.presentation.databinding.ItemGithubRepoBinding

class GithubRepoAdapter(private val githubRepoItemClickListener: GithubRepoItemClickListener) :
    PagingDataAdapter<GithubRepo, GithubRepoAdapter.MyViewHolder>(diffUtil) {

    inner class MyViewHolder(private val binding: ItemGithubRepoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cardView.setOnClickListener {
                githubRepoItemClickListener.onItemClicked(getItem(absoluteAdapterPosition)!!)
            }
        }

        fun bind(githubRepo: GithubRepo) {
            binding.githubRepo = githubRepo
            Log.d("TTTTTT", "뷰홀더 바인딩 완료. ${githubRepo.ownerProfile}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d("TTTTTT", "뷰홀더 생성")
        val binding = ItemGithubRepoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Log.d("TTTTTT", "뷰홀더 바인딩")
        getItem(position)?.let { holder.bind(it) }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<GithubRepo>() {
            override fun areItemsTheSame(oldItem: GithubRepo, newItem: GithubRepo): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: GithubRepo, newItem: GithubRepo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
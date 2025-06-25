package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.databinding.LoadStateBinding

class PagingLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<PagingLoadStateAdapter.StateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState) =
        StateViewHolder(
            LoadStateBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            retry
        )

    override fun onBindViewHolder(holder: StateViewHolder, loadState: LoadState) =
        holder.bind(loadState)

    class StateViewHolder(
        private val binding: LoadStateBinding,
        retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.retry.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) {
            binding.progress.isVisible = loadState is LoadState.Loading
            binding.retry.isVisible = loadState is LoadState.Error
        }
    }
}

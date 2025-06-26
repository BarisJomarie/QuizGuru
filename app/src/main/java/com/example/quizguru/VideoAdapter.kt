package com.example.quizguru

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
  private val videoList: List<Video>,
  private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

  class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.tvVideoTitle)
    val description: TextView = itemView.findViewById(R.id.tvVideoDescription)
    val theme: TextView = itemView.findViewById(R.id.tvVideoTheme)

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_video, parent, false)
    return VideoViewHolder(view)
  }

  override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
    val video = videoList[position]
    holder.title.text = video.title
    holder.description.text = video.description
    holder.theme.text = "Theme: ${video.theme}"

    holder.itemView.setOnClickListener {
      onItemClick(video)
    }
  }

  override fun getItemCount() = videoList.size
  }
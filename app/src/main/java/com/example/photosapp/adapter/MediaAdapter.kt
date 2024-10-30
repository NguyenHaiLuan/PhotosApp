package com.example.photosapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.databinding.ItemMediaBinding
import com.example.photosapp.model.Media

class MediaAdapter (
    val context: Context,
    val mediaList: List<Media>,
    private val onMediaClick: (Media) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MyViewHolder>() {

    class MyViewHolder (val binding: ItemMediaBinding): ViewHolder(binding.root){
        val media = binding.media
        val videoLabel = binding.videoLabel
        val timeVideo = binding.txtTimeOfVideo
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MyViewHolder(ItemMediaBinding.bind(view))
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val mediaItem = mediaList[position]

        Glide.with(context)
            .load(mediaItem.uri)
            .placeholder(R.drawable.img_place_holder)
            .into(holder.media)

        // Kiểm tra nếu là video thì hiển thị biểu tượng video
        if (mediaItem.isVideo) {
            holder.videoLabel.visibility = View.VISIBLE
            holder.timeVideo.visibility = View.VISIBLE
        } else {
            holder.videoLabel.visibility = View.GONE
            holder.timeVideo.visibility = View.GONE
        }

        holder.media.setOnClickListener {
            onMediaClick(mediaItem) // Gọi hàm callback để xử lý sự kiện click
        }

    }
}
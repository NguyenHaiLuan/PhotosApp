package com.example.photosapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.databinding.ItemPhotoBinding
import com.example.photosapp.model.Media

class MediaAdapter (
    val context: Context,
    val mediaList: List<Media>
) : RecyclerView.Adapter<MediaAdapter.MyViewHolder>() {

    class MyViewHolder (val binding: ItemPhotoBinding): ViewHolder(binding.root){
        val media = binding.media
        val videoLabel = binding.videoLabel
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return MyViewHolder(ItemPhotoBinding.bind(view))
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val photo = mediaList[position]

        Glide.with(context)
            .load(photo.uri)
            .placeholder(R.drawable.img_place_holder)
            .into(holder.media)

        // Kiểm tra nếu là video thì hiển thị biểu tượng video
        if (photo.isVideo) {
            holder.videoLabel.visibility = View.VISIBLE
        } else {
            holder.videoLabel.visibility = View.GONE
        }
    }
}
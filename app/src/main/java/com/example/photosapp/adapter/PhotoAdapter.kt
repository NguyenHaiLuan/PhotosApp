package com.example.photosapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.databinding.ItemPhotoBinding
import com.example.photosapp.model.Photo

class PhotoAdapter (
    val context: Context,
    val photoList: List<Photo>
) : RecyclerView.Adapter<PhotoAdapter.MyViewHolder>() {

    class MyViewHolder (val binding: ItemPhotoBinding): ViewHolder(binding.root){
        val img = binding.imgPhoto
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return MyViewHolder(ItemPhotoBinding.bind(view))
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val photo = photoList[position]

        Glide.with(context)
            .load(photo.uri)
            .placeholder(R.drawable.img_place_holder)
            .into(holder.img)

    }
}
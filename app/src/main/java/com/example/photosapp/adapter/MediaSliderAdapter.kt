package com.example.photosapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.photosapp.fragment.MediaFragment
import com.example.photosapp.model.Media

class MediaSliderAdapter(
    fragmentActivity: FragmentActivity,
    private val mediaList: List<Media>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = mediaList.size

    override fun createFragment(position: Int): Fragment {
        val mediaItem = mediaList[position]
        return MediaFragment.newInstance(mediaItem)
    }
}

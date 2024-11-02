package com.example.photosapp.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.photosapp.databinding.FragmentMediaBinding
import com.example.photosapp.model.Media

class MediaFragment : Fragment() {

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    private lateinit var media: Media

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)

        if (::media.isInitialized) {
            val uri = media.uri
            if (media.isVideo) {
                showVideo(uri)
            } else {
                showImage(uri)
            }
        }

        return binding.root
    }

    private fun showImage(mediaUri: Uri) {
        binding.photoView.setImageURI(mediaUri)
        binding.photoView.visibility = View.VISIBLE
        binding.videoView.visibility = View.GONE
    }

    private fun showVideo(mediaUri: Uri?) {
        if (mediaUri == null) {
            return
        }

        binding.videoView.visibility = View.VISIBLE
        binding.photoView.visibility = View.GONE

        // Đặt URI cho video
        try {
            binding.videoView.setVideoURI(mediaUri)
        } catch (e: Exception) {
            Log.e("MediaFragment", "Failed to set video URI: ${e.message}")
            return
        }

        // Kiểm tra tỷ lệ video khi chuẩn bị xong
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            val videoWidth = mediaPlayer.videoWidth
            val videoHeight = mediaPlayer.videoHeight
            val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

            // Lấy kích thước của màn hình
            val layoutParams = binding.videoView.layoutParams
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Kiểm tra và điều chỉnh tỷ lệ khung hình cho video
            if (videoWidth > videoHeight) { // Video nằm ngang
                layoutParams.width = screenWidth
                layoutParams.height = (screenWidth / videoAspectRatio).toInt()
            } else { // Video đứng hoặc vuông
                layoutParams.height = screenHeight
                layoutParams.width = (screenHeight * videoAspectRatio).toInt()
            }

            binding.videoView.layoutParams = layoutParams
            binding.videoView.requestLayout() // Cập nhật layout ngay lập tức

            // Phát video sau khi layout đã cập nhật
            mediaPlayer.start()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(media: Media): MediaFragment {
            val fragment = MediaFragment()
            fragment.media = media
            return fragment
        }
    }
}

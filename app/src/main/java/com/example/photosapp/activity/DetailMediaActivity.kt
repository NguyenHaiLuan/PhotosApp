package com.example.photosapp.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.databinding.ActivityDetailMediaBinding
import com.example.photosapp.utils.invisible
import com.example.photosapp.utils.visible

class DetailMediaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailMediaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        // Nhận dữ liệu từ Intent
        val uriString = intent.getStringExtra("uri")
        val nameString = intent.getStringExtra("nameMedia")
        val isVideo = intent.getBooleanExtra("isVideo", false)
        val mediaUri = Uri.parse(uriString)

        binding.mediaName.text = nameString

        binding.btnBack.setOnClickListener {
            finish()
        }

        if (isVideo) {
            showVideo(mediaUri)
        } else {
            showImage(mediaUri)
        }
    }

    private fun showImage(mediaUri: Uri?) {
        binding.videoView.visibility = View.GONE
        binding.photoView.visibility = View.VISIBLE

        Glide.with(this)
            .load(mediaUri)
            .placeholder(R.drawable.img_place_holder)
            .into(binding.photoView)
    }

    private fun showVideo(mediaUri: Uri?) {
        binding.videoView.visibility = View.VISIBLE
        binding.photoView.visibility = View.GONE

        // Đặt URI cho video
        binding.videoView.setVideoURI(mediaUri)

        // Lắng nghe sự kiện khi video đã chuẩn bị xong
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            // Lấy kích thước của video
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

            // Áp dụng các thay đổi cho VideoView
            binding.videoView.layoutParams = layoutParams

            // Bắt đầu phát video
            mediaPlayer.start()
        }
    }

    private fun initUI() {
        enableEdgeToEdge()
        binding = ActivityDetailMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
package com.example.photosapp.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.adapter.MediaSliderAdapter
import com.example.photosapp.databinding.ActivityDetailMediaBinding
import com.example.photosapp.model.Media
import com.example.photosapp.utils.invisible
import com.example.photosapp.utils.visible

class DetailMediaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailMediaBinding
    private lateinit var mediaList: List<Media>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        // Nhận danh sách media từ intent
        mediaList = intent.getParcelableArrayListExtra("mediaList") ?: listOf()

        // Thiết lập ViewPager2 với Adapter
        val adapter = MediaSliderAdapter(this, mediaList)
        binding.viewPager.adapter = adapter

        // Chuyển tới vị trí media được chọn
        val startPosition = intent.getIntExtra("startPosition", 0)
        binding.viewPager.setCurrentItem(startPosition, false)

        binding.mediaName.text = mediaList[startPosition].name


        // lang nghe su thay doi cua view pager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.mediaName.text = mediaList[position].name
            }
        })
        binding.btnBack.setOnClickListener{
            finish()
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

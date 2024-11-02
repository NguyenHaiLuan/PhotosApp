package com.example.photosapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.R
import com.example.photosapp.adapter.MediaAdapter
import com.example.photosapp.databinding.ActivityMainBinding
import com.example.photosapp.model.Media

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MediaAdapter
    private val mediaList = mutableListOf<Media>()

    @SuppressLint("NotifyDataSetChanged")
    private val detailActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Cập nhật danh sách media khi có thay đổi từ Detail Activity
            mediaList.clear()
            mediaList.addAll(loadAllMedia())
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        setUpRecyclerView()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setUpRecyclerView() {
        mediaList.addAll(loadAllMedia())
        adapter = MediaAdapter(this, mediaList) { mediaItem ->
            val intent = Intent(this, DetailMediaActivity::class.java)
            intent.putParcelableArrayListExtra("mediaList", ArrayList(mediaList))
            intent.putExtra("startPosition", mediaList.indexOf(mediaItem))
            detailActivityLauncher.launch(intent) // Sử dụng launcher để nhận kết quả
        }
        binding.listImageRecyclerView.adapter = adapter
        binding.listImageRecyclerView.layoutManager = GridLayoutManager(this, 4)
    }

    private fun loadAllMedia(): List<Media> {
        val tempList = mutableListOf<Media>()

        // Truy vấn ảnh từ MediaStore
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        contentResolver.query(imageUri, imageProjection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
            .use { cursor ->
                cursor?.let {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                        tempList.add(Media(id, displayName, uri, isVideo = false, dateAdded = dateAdded))
                    }
                }
            }

        // Truy vấn video từ MediaStore
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        contentResolver.query(videoUri, videoProjection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")
            .use { cursor ->
                cursor?.let {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
                        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                        tempList.add(Media(id, displayName, uri, isVideo = true, dateAdded = dateAdded, duration = duration))
                    }
                }
            }

        return tempList.sortedByDescending { it.dateAdded }
    }

    private fun initUI() {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

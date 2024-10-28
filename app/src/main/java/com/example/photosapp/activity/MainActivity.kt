package com.example.photosapp.activity

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.R
import com.example.photosapp.adapter.PhotoAdapter
import com.example.photosapp.databinding.ActivityMainBinding
import com.example.photosapp.model.Photo
import java.io.File
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PhotoAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        setUpRecyclerView() // Set up recycler View để load all hình ảnh

        binding.btnAddPhoto.setOnClickListener {
            finish()
        }
    }

    private fun setUpRecyclerView() {
        val photoList = loadAllImages()
        adapter = PhotoAdapter(this, photoList)
        binding.listImageRecyclerView.adapter = adapter
        binding.listImageRecyclerView.layoutManager = GridLayoutManager(this, 4)
    }

    private fun loadAllImages() : List<Photo>{
        var tempList = mutableListOf<Photo>()
        val uri = when{
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->{
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            else->{
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Sắp xếp theo DATE_ADDED giảm dần để lấy ảnh mới nhất lên trước
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(uri, projection, null, null, sortOrder)
            .use {cursor->
                cursor?.let {
                    while (cursor.moveToNext()){
                        val photoId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId)
                        val photo = Photo(photoId, displayName, uri)

                        tempList.add(photo)
                    }
                }

            }
        return tempList
    }

    private fun initUI(){
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllImages()
    }
}
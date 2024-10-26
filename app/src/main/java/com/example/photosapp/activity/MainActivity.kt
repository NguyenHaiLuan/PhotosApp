package com.example.photosapp.activity

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PhotoAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        //yêu cầu cấp quyền
        checkUserPermission()
    }

    private fun checkUserPermission() {
        // kiểm tra ứng dụng có được cấp quyền
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
             ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),69)
         } else{
             setUpRecyclerView()
         }
    }

    private fun setUpRecyclerView() {
        val photoList = loadAllImages()
        adapter = PhotoAdapter(this, photoList)
        binding.listImageRecyclerView.adapter = adapter
        binding.listImageRecyclerView.layoutManager = GridLayoutManager(this, 4)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 69) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.textNotify.visibility = View.GONE
                loadAllImages()
            } else {
                // Hiển thị thông báo và button nếu quyền bị từ chối
                binding.textNotify.visibility = View.VISIBLE
            }
        }
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

        )

        contentResolver.query(uri, projection, null, null, null)
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
        //nếu đã được cấp quyền thì list ảnh ra
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            binding.textNotify.visibility = View.GONE
            setUpRecyclerView()
        }
    }
}
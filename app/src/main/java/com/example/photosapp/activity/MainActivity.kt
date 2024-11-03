package com.example.photosapp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.R
import com.example.photosapp.adapter.MediaAdapter
import com.example.photosapp.databinding.ActivityMainBinding
import com.example.photosapp.model.Media
import com.example.photosapp.utils.appSettingOpen
import com.example.photosapp.utils.warningPermissionDialog

class MainActivity : AppCompatActivity() {
    // ----------------------------------------- KHAI BAO---------------------------------------------------

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MediaAdapter
    private val mediaList = mutableListOf<Media>()

    private val multiplePermissionNameList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }

    @SuppressLint("NotifyDataSetChanged")
    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Cập nhật danh sách media khi có thay đổi từ Detail Activity
                mediaList.clear()
                mediaList.addAll(loadAllMedia())
                adapter.notifyDataSetChanged()
            }
        }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 639
    }
    //----------------------------------------------------ON CREATE()----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        if (checkMultiplePermission()){
            setUpRecyclerView()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    //--------------------------------------------------METHODS---------------------------------------------------

    // Kiểm tra quyền có trong danh sách multiplePermissionNameList
    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        return if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                var allGranted = true
                var permanentlyDenied = false

                for ((index, result) in grantResults.withIndex()) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        allGranted = false
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permissions[index]
                            )
                        ) {
                            permanentlyDenied = true
                        }
                    }
                }

                if (allGranted) {
                    setUpRecyclerView()
                } else {
                    if (permanentlyDenied) {
                        // Mở cài đặt ứng dụng vì có quyền bị từ chối vĩnh viễn
                        appSettingOpen(this)
                    } else {
                        // Hiển thị cảnh báo yêu cầu cấp quyền
                        warningPermissionDialog(this) { _, which ->
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                checkMultiplePermission()
                            }
                        }
                    }
                }
            }
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

        contentResolver.query(
            imageUri,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
            .use { cursor ->
                cursor?.let {
                    while (cursor.moveToNext()) {
                        val id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val displayName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val dateAdded =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()
                        tempList.add(
                            Media(
                                id,
                                displayName,
                                uri,
                                isVideo = false,
                                dateAdded = dateAdded
                            )
                        )
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
        contentResolver.query(
            videoUri,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
            .use { cursor ->
                cursor?.let {
                    while (cursor.moveToNext()) {
                        val id =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val displayName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        val dateAdded =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
                        val duration =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()
                        tempList.add(
                            Media(
                                id,
                                displayName,
                                uri,
                                isVideo = true,
                                dateAdded = dateAdded,
                                duration = duration
                            )
                        )
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

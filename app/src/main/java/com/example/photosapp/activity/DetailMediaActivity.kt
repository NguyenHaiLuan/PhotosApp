package com.example.photosapp.activity

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.photosapp.R
import com.example.photosapp.adapter.MediaSliderAdapter
import com.example.photosapp.databinding.ActivityDetailMediaBinding
import com.example.photosapp.dialog.DeleteMediaDialog
import com.example.photosapp.dialog.RenameMediaDialog
import com.example.photosapp.model.Media
import com.example.photosapp.utils.appSettingOpen
import com.example.photosapp.utils.warningPermissionDialog
import com.soundcloud.android.crop.Crop
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.launch
import java.io.File

class DetailMediaActivity : AppCompatActivity(), RenameMediaDialog.RenameMediaListener {
    private lateinit var binding: ActivityDetailMediaBinding
    private lateinit var mediaList: MutableList<Media>
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var adapter:MediaSliderAdapter?=null;

    private val multiplePermissionNameList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            arrayListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }

    companion object {
        const val BITMAP_QUALITY = 100
        const val PERMISSION_REQUEST_CODE = 333
    }

    // -----------------------------------------CODE-----------------------------------------------

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        // Nhận data từ intent
        mediaList = intent.getParcelableArrayListExtra<Media>("mediaList")?.toMutableList()
            ?: mutableListOf()
        val startPosition = intent.getIntExtra("startPosition", 0)

        // Thiết lập ViewPager2 với Adapter
        adapter = MediaSliderAdapter(this, mediaList)
        if (checkMultiplePermission()) {
            binding.viewPager.adapter = adapter
            binding.viewPager.setCurrentItem(startPosition, false)
            binding.mediaName.text = mediaList[startPosition].name

            // thiết lập các nút lệnh cho ảnh hoặc video
            initEditButton(startPosition)
        }

        binding.btnBack.setOnClickListener { finish() }

        // Sự kiện cho nút xóa media
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteMediaDialog()
            deleteDialog.showDialog(this) {
                // Xác nhận xóa
                lifecycleScope.launch {
                    val position = binding.viewPager.currentItem
                    val mediaUri = mediaList.getOrNull(position)?.uri

                    if (mediaUri != null) {
                        deleteMedia(mediaUri)
                    } else {
                        StyleableToast.makeText(
                            this@DetailMediaActivity,
                            getString(R.string.delete_fail_message),
                            R.style.error_toast
                        ).show()
                    }
                }
            }
        }

        // Sự kiện cho nút đổi tên media
        binding.btnRename.setOnClickListener {
            val renameDialog = RenameMediaDialog()
            renameDialog.show(supportFragmentManager, "RenameMediaDialog")
        }

        // Sự kiện cho nút share
        binding.btnShare.setOnClickListener {
            val position = binding.viewPager.currentItem
            val mediaUri = mediaList[position].uri
            shareMedia(mediaUri)
        }

        //Sự kiện cho nút Crop
        binding.btnCrop.setOnClickListener {
            startCrop(mediaList[startPosition].uri)
        }

        //Sự kiện cho nút ColorFilter
        binding.btnColorFilter.setOnClickListener {
            val intent = Intent(this@DetailMediaActivity, ColorFilterImageActivity::class.java)
            intent.putExtra("mediaNeed", mediaList[startPosition])
            startActivity(intent)
        }

        // Xử lý kết quả sau khi thực hiện IntentSender để xóa
        intentSenderLauncher = registerForActivityResult(StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val currentPos = binding.viewPager.currentItem
                if (currentPos in mediaList.indices) {
                    mediaList.removeAt(currentPos)
                    adapter?.notifyItemRemoved(currentPos)

                    if (mediaList.isEmpty()) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        // Cập nhật ViewPager với media kế tiếp
                        binding.viewPager.setCurrentItem(
                            currentPos.coerceAtMost(mediaList.size - 1),
                            false
                        )
                        binding.mediaName.text = mediaList[binding.viewPager.currentItem].name
                    }
                } else {
                    StyleableToast.makeText(
                        this,
                        getString(R.string.cant_delete_video_message),
                        R.style.error_toast
                    ).show()
                }
            }
        }


        // lắng nghe thay đổi của viewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                binding.mediaName.text = mediaList[position].name
                if (mediaList[position].isVideo) {
                    binding.btnCrop.visibility = View.GONE
                    binding.btnColorFilter.visibility = View.GONE
                } else {
                    binding.btnCrop.visibility = View.VISIBLE
                    binding.btnColorFilter.visibility = View.VISIBLE
                }
            }
        })
    }

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
                    StyleableToast.makeText(
                        this,
                        getString(R.string.success_message),
                        R.style.success_toast
                    ).show()
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

    private fun startCrop(sourceUri: Uri) {
        Crop.of(sourceUri, Uri.fromFile(File(cacheDir, "cropped_image.jpg"))).asSquare().start(this)
    }

    private fun shareMedia(uri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = contentResolver.getType(uri) // Lấy loại media
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Cấp quyền đọc URI
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
    }

    private fun initEditButton(position: Int) {
        if (mediaList[position].isVideo) {
            binding.btnCrop.visibility = View.GONE
            binding.btnColorFilter.visibility = View.GONE
        } else {
            binding.btnCrop.visibility = View.VISIBLE
            binding.btnColorFilter.visibility = View.VISIBLE
        }
    }

    // xử lý nut Ok khi rename
    override fun onRenameConfirmed(newName: String) {
        val position = binding.viewPager.currentItem
        val media = mediaList[position]

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            }
            val rowsUpdated = contentResolver.update(media.uri, contentValues, null, null)

            if (rowsUpdated > 0) {
                StyleableToast.makeText(
                    this,
                    getString(R.string.rename_success_message),
                    R.style.success_toast
                ).show()
                mediaList[position] = media.copy(name = newName)
                binding.mediaName.text = newName
                MediaScannerConnection.scanFile(this, arrayOf(media.uri.toString()), null, null)

                // Trả lại tên mới cho MainActivity
                val resultIntent = Intent()
                resultIntent.putExtra("renamedImage", newName)
                setResult(Activity.RESULT_OK, resultIntent)
            } else {
                StyleableToast.makeText(this, getString(R.string.rename_fail_message), R.style.warning_toast).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            StyleableToast.makeText(this, getString(R.string.error_rename_message), R.style.error_toast).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = Crop.getOutput(data)
            saveCroppedImageToGallery(resultUri)
        }
    }

    private fun saveCroppedImageToGallery(uri: Uri) {
        // Lưu ảnh đã cắt vào thư viện ảnh
        val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "cropped_image_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uriResult = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uriResult?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, outputStream)
                }
            }
            StyleableToast.makeText(
                this,
                getString(R.string.crop_image_success_message),
                R.style.success_toast
            ).show()

            // Trả lại URI mới cho MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("newImageUri", uriResult.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    // Hàm xóa media
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMedia(uri: Uri) {
        val resolver = contentResolver

        val currentPos = binding.viewPager.currentItem
        if (currentPos in mediaList.indices) {
            // Xóa item khỏi danh sách dữ liệu
            mediaList.removeAt(currentPos)
            // Thông báo cho adapter về sự thay đổi
            adapter?.notifyItemRemoved(currentPos)
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )

                // Trả lại URI mới cho MainActivity
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            else -> {
                try {
                    resolver.delete(uri, null, null)
                    StyleableToast.makeText(
                        this,
                        getString(R.string.delete_success_message),
                        R.style.success_toast
                    ).show()
                    // Trả lại URI mới cho MainActivity
                    val resultIntent = Intent()
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    StyleableToast.makeText(this, getString(R.string.delete_fail_message), R.style.error_toast).show()
                }
            }
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



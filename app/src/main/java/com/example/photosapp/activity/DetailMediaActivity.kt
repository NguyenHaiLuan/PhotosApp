package com.example.photosapp.activity

import android.app.Activity
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.launch

class DetailMediaActivity : AppCompatActivity(), RenameMediaDialog.RenameMediaListener {
    private lateinit var binding: ActivityDetailMediaBinding
    private lateinit var mediaList: MutableList<Media> // Đổi thành MutableList
    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        // Nhận data từ intent
        mediaList = intent.getParcelableArrayListExtra<Media>("mediaList")?.toMutableList()
            ?: mutableListOf()
        val startPosition = intent.getIntExtra("startPosition", 0)

        // Thiết lập ViewPager2 với Adapter
        val adapter = MediaSliderAdapter(this, mediaList)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)
        binding.mediaName.text = mediaList[startPosition].name

        if (mediaList[startPosition].isVideo) {
            binding.btnCrop.visibility = View.GONE
            binding.btnColorFilter.visibility = View.GONE
        } else {
            binding.btnCrop.visibility = View.VISIBLE
            binding.btnColorFilter.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener { finish() }

        // Sự kiện cho nút xóa media
        binding.btnDelete.setOnClickListener {
            val deleteDialog = DeleteMediaDialog()
            deleteDialog.showDialog(this) {
                // Xác nhận xóa
                lifecycleScope.launch {
                    val position = binding.viewPager.currentItem
                    val mediaUri = mediaList[position].uri
                    deleteMedia(mediaUri)
                }
            }
        }

        // Sự kiện cho nút đổi tên media
        binding.btnRename.setOnClickListener {
            val renameDialog = RenameMediaDialog()
            renameDialog.show(supportFragmentManager, "RenameMediaDialog")
        }

        // Xử lý kết quả sau khi thực hiện IntentSender để xóa
        intentSenderLauncher = registerForActivityResult(StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val currentPos = binding.viewPager.currentItem
                if (currentPos in mediaList.indices) {
                    mediaList.removeAt(currentPos)
                    adapter.notifyItemRemoved(currentPos)

                    if (mediaList.isEmpty()) finish()
                    else {
                        binding.viewPager.setCurrentItem(
                            currentPos.coerceAtMost(mediaList.size - 1),
                            false
                        )
                        binding.mediaName.text = mediaList[binding.viewPager.currentItem].name
                    }
                } else {
                    StyleableToast.makeText(
                        this,
                        "Không thể xóa media. Vị trí không hợp lệ.",
                        R.style.error_toast
                    ).show()
                }
                setResult(Activity.RESULT_OK)
            }
        }

        // lắng nghe thay đổi của viewPager
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

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
                StyleableToast.makeText(this, "Đổi tên thành công", R.style.success_toast).show()
                mediaList[position] = media.copy(name = newName)
                binding.mediaName.text = newName
                MediaScannerConnection.scanFile(this, arrayOf(media.uri.toString()), null, null)
            } else {
                StyleableToast.makeText(this, "Không thể đổi tên media", R.style.error_toast).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            StyleableToast.makeText(this, "Đổi tên thất bại: " + e.message, R.style.error_toast)
                .show()
        }
    }

    // Hàm xóa media
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMedia(uri: Uri) {
        val resolver = contentResolver

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }

            else -> {
                try {
                    resolver.delete(uri, null, null)
                    StyleableToast.makeText(this, "Xóa thành công!", R.style.success_toast).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    StyleableToast.makeText(this, "Xóa không thành công!", R.style.error_toast)
                        .show()
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



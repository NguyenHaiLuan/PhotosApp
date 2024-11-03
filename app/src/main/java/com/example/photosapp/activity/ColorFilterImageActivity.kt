package com.example.photosapp.activity

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.photosapp.R
import com.example.photosapp.adapter.ColorFilterAdapter
import com.example.photosapp.databinding.ActivityColorFilterImageBinding
import com.example.photosapp.model.Media
import com.example.photosapp.utils.FilterUtils
import io.github.muddz.styleabletoast.StyleableToast

class ColorFilterImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityColorFilterImageBinding
    private val filterList = listOf(NONE, BRIGHTENING, VIBRANCY, CONTRAST, GRAY_SCALE, SEPIA, RED, GREEN, BLUE)
    private lateinit var adapter: ColorFilterAdapter
    private lateinit var media: Media

    companion object {
        const val NONE = "Không"
        const val RED = "Đỏ"
        const val GREEN = "Xanh lá"
        const val BLUE = "Xanh"
        const val SEPIA = "Sepia"
        const val GRAY_SCALE = "Xám"
        const val BRIGHTENING = "Tươi sáng"
        const val CONTRAST = "Tương phản"
        const val VIBRANCY = "Ấm hơn"
        const val INTEND_CODE = "mediaNeed"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        media = intent.getParcelableExtra(INTEND_CODE)!!

        // Thiết lập hình ảnh vào ImageView
        Glide.with(this).load(media.uriString).into(binding.imageView)
        setupRecyclerView()

        // lắng nghe sự kiên cho các button
        setupListeners()
    }


    private fun setupRecyclerView() {
        adapter = ColorFilterAdapter(filterList) { filterName ->
            applyFilter(filterName)
        }

        binding.filterRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.filterRecyclerView.adapter = adapter
    }


    private fun applyFilter(filterName: String) {
        // Áp dụng bộ lọc màu cho ImageView
        when (filterName) {
            NONE -> binding.imageView.colorFilter = FilterUtils.getNoneFilter()

            RED -> binding.imageView.colorFilter = FilterUtils.getRedFilter()

            GREEN -> binding.imageView.colorFilter = FilterUtils.getGreenFilter()

            BLUE -> binding.imageView.colorFilter = FilterUtils.getBlueFilter()

            SEPIA -> binding.imageView.colorFilter = FilterUtils.getSepiaFilter()

            GRAY_SCALE -> binding.imageView.colorFilter = FilterUtils.getGrayScaleFilter()

            BRIGHTENING -> binding.imageView.colorFilter = FilterUtils.getBrighteningFilter(30f)

            CONTRAST -> binding.imageView.colorFilter = FilterUtils.getContrastFilter(5f)

            VIBRANCY -> binding.imageView.colorFilter = FilterUtils.getVibranceFilter(2f)

            else -> binding.imageView.clearColorFilter()
        }
    }


    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveImageWithFilter()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

    }


    private fun saveImageWithFilter() {
        val bitmap = getBitmapFromImageView(binding.imageView)

        // Lưu ảnh vào bộ nhớ
        val values = ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "IMG_Ft_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES
            )

        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                StyleableToast.makeText(this, getString(R.string.take_photo_success_message), R.style.success_toast).show()
            }

        } ?: StyleableToast.makeText(this, getString(R.string.save_image_fail_message), R.style.error_toast).show()

        // Trả lại tên mới cho MainActivity
        val resultIntent = Intent()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }


    private fun getBitmapFromImageView(imageView: ImageView): Bitmap {
        imageView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = false
        return bitmap
    }


    private fun initUI() {
        enableEdgeToEdge()
        binding = ActivityColorFilterImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
package com.example.photosapp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.photosapp.R
import com.example.photosapp.databinding.ActivityCameraBinding
import com.example.photosapp.utils.appSettingOpen
import com.example.photosapp.utils.invisible
import com.example.photosapp.utils.visible
import com.example.photosapp.utils.warningPermissionDialog
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {

    //------------------------------ KHAI BÁO BIẾN---------------------------------------------
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var aspectRatio = AspectRatio.RATIO_16_9
    private var orientationEventListener: OrientationEventListener? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isPhoto = true
    private var isBackClicked = false
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var recordingStartTime: Long = 0L
    //List các quyền cần cấp

    private val multiplePermissionNameList =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        } else {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 69
        const val RATIO_ZOOM = 0.5f
        const val MIN_RATIO_ZOOM = 1f
    }

    // ---------------------------------CODE------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        if (checkMultiplePermission()) {
            startCamera()
        }

        //---------------lắng nghe sự kiện các nút-----------
        //nút chụp ảnh
        binding.btnTakePhoto.setOnClickListener {
            if (isPhoto) {
                takePhoto()
            } else {
                captureVideo()
            }
        }

        //nút đổi camera
        binding.btnFlipCamera.setOnClickListener {
            flipCamera()
        }
        //nút flash
        binding.btnFlash.setOnClickListener {
            turnFlash()
        }

        //nút thay đổi tỉ lệ khung hình
        binding.txtRatioAspect.setOnClickListener {
            changeAspectRatio()
        }

        //nút thay đổi camera mode
        binding.btnChangeModeOfCamera.setOnClickListener {
            changeModeOfCamera()
        }

        //sự kiện khi nhấn vào ảnh gần đây
        binding.imgViewRecentImage.setOnClickListener {
            val intent = Intent(this@CameraActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun bindCameraUseCases() {
        // lấy hướng xoay hiện tại của màn hình
        val rotation = getDisplayRotation()

        //hiển thị luồng xem trước (preview) từ camera trên màn hình
        val preview = Preview.Builder()
            .setTargetRotation(rotation) // Thiết lập hướng xoay cho preview khớp với hướng màn hình
            .build()
            .also {
                it.surfaceProvider = binding.viewFinder.surfaceProvider
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .setAspectRatio(aspectRatio)
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .build()

        // xác định camera nào sẽ được sử dụng (camera trước hoặc sau)
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()

            // liên kết các use case (preview, imageCapture, videoCapture) với camera được chọn
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            setUpZoom()
            setupZoomButtons()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureVideo() {
        binding.btnTakePhoto.isEnabled = false
        binding.btnFlash.invisible()
        binding.btnFlipCamera.invisible()
        binding.btnChangeModeOfCamera.invisible()
        binding.txtRatioAspect.invisible()
        binding.imgViewRecentImage.invisible()

        recording?.let {
            it.stop()
            recording = null
            return
        }

        val fileName = "VID_" + SimpleDateFormat(
            getString(R.string.date_time_format),
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".mp4"

        val contentValue = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValue)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        this@CameraActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.btnTakePhoto.setImageResource(R.drawable.baseline_stop_circle_24)
                        binding.btnTakePhoto.isEnabled = true
                        recordingStartTime = System.currentTimeMillis()
                        startRecordingTimer()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val message = getString(R.string.capture_video_success_message)
                            StyleableToast.makeText(this, message, R.style.success_toast).show()
                        } else {
                            recording?.close()
                            recording = null
                        }
                        stopRecordingTimer()
                        binding.btnTakePhoto.setImageResource(R.drawable.baseline_fiber_manual_record_24)
                        binding.btnTakePhoto.isEnabled = true
                        binding.btnFlash.visible()
                        binding.btnFlipCamera.visible()
                        binding.btnChangeModeOfCamera.visible()
                        binding.txtRatioAspect.visible()
                        binding.imgViewRecentImage.visible()
                    }
                }
            }
    }

    private fun takePhoto() {
        val imageFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "CameraBC"
        )

        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        // tên file: IMG_2024-06-09_6-9-69.jpg
        val fileName = "IMG_" + SimpleDateFormat(
            getString(R.string.date_time_format),
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".jpeg"

        val contentValue = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraBC")
            }
        }

        // Chống ảnh bị lật ngược khi chụp bằng camera trước
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        val outOption =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValue
                )
                    .setMetadata(metadata)
                    .build()
            } else {
                val imageFile = File(imageFolder, fileName)
                OutputFileOptions.Builder(imageFile)
                    .setMetadata(metadata)
                    .build()
            }

        imageCapture.takePicture(
            outOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return

                    StyleableToast.makeText(
                        this@CameraActivity,
                        getString(R.string.take_photo_success_message),
                        R.style.success_toast
                    ).show()
                    // Cập nhật MediaStore với file vừa chụp
                    MediaScannerConnection.scanFile(
                        this@CameraActivity,
                        arrayOf(savedUri.path),
                        null
                    ) { path, uri ->
                        Log.d(
                            "CameraActivity",
                            "${getString(R.string.check_uri_string)} $path, Uri: $uri"
                        )
                    }
                    Log.d("image_uri", "${outputFileResults.savedUri}")
                }

                override fun onError(exception: ImageCaptureException) {
                    StyleableToast.makeText(
                        this@CameraActivity,
                        "Lỗi: ${exception.message}",
                        R.style.error_toast
                    ).show()
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
                    startCamera()
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


    private fun startRecordingTimer() {
        binding.txtTimeRecording.visible()
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            @SuppressLint("DefaultLocale")
            override fun run() {
                val elapsedMillis = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / (1000 * 60)) % 60
                binding.txtTimeRecording.text = String.format("%02d:%02d", minutes, seconds)
                timerHandler?.postDelayed(this, 1000) // Cập nhật mỗi giây
            }
        }
        timerHandler?.post(timerRunnable as Runnable)
    }

    // dừng bộ đếm thời gian
    private fun stopRecordingTimer() {
        timerRunnable?.let { timerHandler?.removeCallbacks(it) }
        binding.txtTimeRecording.invisible()
    }


    private fun setUpZoom() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @SuppressLint("SetTextI18n")
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                val newZoomRatio = currentZoomRatio * delta
                camera.cameraControl.setZoomRatio(newZoomRatio)

                camera.cameraInfo.zoomState.observe(this@CameraActivity) { zoomState ->
                    val zoomRatio = zoomState.zoomRatio
                    binding.txtZoomRatio.text = "${"%.1f".format(zoomRatio)}x"
                }
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this, listener)

        binding.viewFinder.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = binding.viewFinder.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(2, TimeUnit.SECONDS)
                    .build()

                camera.cameraControl.startFocusAndMetering(action)
                view.performClick()
            }
            true
        }
    }

    private fun setupZoomButtons() {

        binding.btnZoomIn.setOnClickListener {
            val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            val newZoomRatio = currentZoomRatio + RATIO_ZOOM // Tăng zoom ratio lên 0.5
            camera.cameraControl.setZoomRatio(
                newZoomRatio.coerceAtMost(
                    camera.cameraInfo.zoomState.value?.maxZoomRatio ?: newZoomRatio
                )
            )
            updateZoomRatioText()
        }

        binding.btnZoomOut.setOnClickListener {
            val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: MIN_RATIO_ZOOM
            val newZoomRatio = currentZoomRatio - RATIO_ZOOM // Giảm zoom ratio xuống 0.5
            camera.cameraControl.setZoomRatio(
                newZoomRatio.coerceAtLeast(
                    camera.cameraInfo.zoomState.value?.minZoomRatio ?: newZoomRatio
                )
            )
            updateZoomRatioText()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateZoomRatioText() {
        camera.cameraInfo.zoomState.observe(this) { zoomState ->
            val zoomRatio = zoomState.zoomRatio
            binding.txtZoomRatio.text = "-- ${"%.1f".format(zoomRatio)}x --"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun changeAspectRatio() {
        if (aspectRatio == AspectRatio.RATIO_16_9) {
            aspectRatio = AspectRatio.RATIO_4_3
            setAspectRatio("H,4:3")
            binding.txtRatioAspect.text = "4:3"
        } else {
            aspectRatio = AspectRatio.RATIO_16_9
            setAspectRatio("H,0:0")
            binding.txtRatioAspect.text = "16:9"
        }
        // Thiết lập lại camera sau khi xử lí xong
        bindCameraUseCases()
    }

    private fun setAspectRatio(ratio: String) {
        binding.viewFinder.layoutParams = binding.viewFinder.layoutParams.apply {
            if (this is ConstraintLayout.LayoutParams) {
                dimensionRatio = ratio
            }
        }
        binding.viewFinder.requestLayout()
    }


    //Xử lí sự kiện khi người dùng bật flash
    private fun setIconFlash(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) { // trạng thái flash = 0 (đang tắt)
                camera.cameraControl.enableTorch(true)
                binding.btnFlash.setImageResource(R.drawable.baseline_flash_off_24)
            } else {
                camera.cameraControl.enableTorch(false)
                binding.btnFlash.setImageResource(R.drawable.baseline_flash_on_24)
            }
        } else if (lensFacing == CameraSelector.LENS_FACING_FRONT) {// Nếu người dùng đang sử dụng camera trước
            StyleableToast.makeText(
                this@CameraActivity,
                getString(R.string.flash_not_available_front_camera_message),
                R.style.error_toast
            ).show()
        } else {// Nếu không có flash
            StyleableToast.makeText(
                this@CameraActivity,
                getString(R.string.unavailable_flash_message),
                R.style.error_toast
            ).show()
        }
    }

    private fun getDisplayRotation(): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        }

        return display?.rotation ?: Surface.ROTATION_0
    }

    // Bắt đầu camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun turnFlash() {
        binding.btnFlash.isEnabled = false
        setIconFlash(camera)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.btnFlash.isEnabled = true // Khôi phục trạng thái nút
        }, 1500)
    }


    private fun flipCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK
        else
            CameraSelector.LENS_FACING_FRONT
        bindCameraUseCases()
    }


    private fun changeModeOfCamera() {
        isPhoto = !isPhoto

        if (isPhoto) {
            binding.btnChangeModeOfCamera.setImageResource(R.drawable.baseline_videocam_24)
            binding.btnTakePhoto.setImageResource(R.drawable.baseline_camera_24)
            binding.txtTimeRecording.visibility = View.GONE
        } else {
            binding.btnChangeModeOfCamera.setImageResource(R.drawable.baseline_camera_24)
            binding.btnTakePhoto.setImageResource(R.drawable.baseline_fiber_manual_record_24)
            binding.txtTimeRecording.visibility = View.VISIBLE
        }
    }

    // xử lí khi người dùng ấn back
    override fun onBackPressed() {
        if (isBackClicked) {
            super.onBackPressed()
            return
        } // thoát ứng dụng

        isBackClicked = true
        StyleableToast.makeText(
            this@CameraActivity,
            getString(R.string.warning_back_message),
            R.style.warning_toast
        ).show()

        // Đặt lại biến isBackClicked về false sau 3 giây
        Handler(Looper.getMainLooper()).postDelayed({
            isBackClicked = false
        }, 3000)
    }

    private fun initUI() {
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // huỷ tài nguyên để ngăn ngừa rỏ rì bộ nhớ
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider.unbindAll()
    }

    override fun onResume() {
        super.onResume()
        orientationEventListener?.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener?.disable()
    }
}
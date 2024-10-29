package com.example.photosapp.activity

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.photosapp.R
import com.example.photosapp.databinding.ActivityCameraBinding
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes.Resolution
import android.provider.MediaStore
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.OutputOptions
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import com.example.photosapp.utils.appSettingOpen
import com.example.photosapp.utils.warningPermissionDialog
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {

    //------------------------------ KHAI BÁO BIẾN---------------------------------------------
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture : ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector : CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var aspectRatio = AspectRatio.RATIO_16_9
    private var orientationEventListener : OrientationEventListener?=null

    private var isBackClicked = false
    //List các quyền cần cấp
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    // ---------------------------------CODE------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        if (checkMultiplePermission()) {
            startCamera()
        }

        //lắng nghe sự kiện các nút
        btnCapture_EventClickListener()  //sự kiện nút chụp ảnh
        btnFlip_EventClickListener()  //sự kiện nút đổi camera
        btnFlash_EventClickListener()  //sự kiện nút flash
        btnChangeAspectRatio_EventClickListener()  //sự kiện nút thay đổi tỉ lệ khung hình

        //sự kiện khi nhấn vào ảnh gần đây
        binding.imgViewRecentImage.setOnClickListener {
            val intent = Intent(this@CameraActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun bindCameraUseCases() {
        val rotation = getDisplayRotation()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

            orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation : Int) {
                // giám sát sự thay đổi của định hướng thiết bị và cập nhật targetRotation của imageCapture
                // để đảm bảo ảnh chụp có định hướng phù hợp.
                imageCapture.targetRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        orientationEventListener?.enable()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun takePhoto() {
        val imageFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraBC")

        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        // tên file: IMG_2024-06-09_6-9-69.jpg
        val fileName = "IMG_"+SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpeg"

        val contentValue = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
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
                        contentValue)
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

                StyleableToast.makeText(this@CameraActivity, "Thành công! Uri của ảnh: $savedUri", R.style.success_toast).show()
                // Cập nhật MediaStore với file vừa chụp
                MediaScannerConnection.scanFile(this@CameraActivity, arrayOf(savedUri.path), null) { path, uri ->
                    Log.d("CameraActivity", "File scanned into MediaStore: $path, Uri: $uri")
                }
                Log.d("image_uri","${outputFileResults.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {
                StyleableToast.makeText(this@CameraActivity, "Lỗi: ${exception.message}", R.style.error_toast).show()
            }
        })
    }

    private fun btnChangeAspectRatio_EventClickListener() {
        binding.txtRatioAspect.setOnClickListener {
            if (aspectRatio  == AspectRatio.RATIO_16_9){
                aspectRatio = AspectRatio.RATIO_4_3
                setAspectRatio("H,4:3")
                binding.txtRatioAspect.text = "4:3"
            } else{
                aspectRatio = AspectRatio.RATIO_16_9
                setAspectRatio("H,0:0")
                binding.txtRatioAspect.text = "16:9"
            }
            // Thiết lập lại camera sau khi xử lí xong
            bindCameraUseCases()
        }
    }

    private fun setAspectRatio(ratio: String) {
        binding.viewFinder.layoutParams = binding.viewFinder.layoutParams.apply {
            if (this is ConstraintLayout.LayoutParams){
                dimensionRatio = ratio
            }
        }
        binding.viewFinder.requestLayout()
    }


    private fun setIconFlash(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()){
            if (camera.cameraInfo.torchState.value == 0){
                camera.cameraControl.enableTorch(true)
                binding.btnFlash.setImageResource(R.drawable.baseline_flash_off_24)
            } else{
                camera.cameraControl.enableTorch(false)
                binding.btnFlash.setImageResource(R.drawable.baseline_flash_on_24)
            }
        }else{
            StyleableToast.makeText(this@CameraActivity, "Flash không khả dụng trên thiết bị này", R.style.error_toast).show()
            binding.btnFlash.isEnabled = false
        }
    }

    private fun getDisplayRotation(): Int {
        val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        }

        return display?.rotation ?: Surface.ROTATION_0
    }

    // Bắt đầu camera
    private fun startCamera(){
        val cameraProviderFeature = ProcessCameraProvider.getInstance(this)
        cameraProviderFeature.addListener({
            cameraProvider = cameraProviderFeature.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    // kiểm tra quyền trong list listPermissionNeeded có được đồng ý hay không
    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionNeeded.toTypedArray(), 609)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 609) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // nếu đáp ứng tất cả các quyền thì khởi chạy camera
                    startCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                        ) {
                            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // mở setting vì có quyền bị từ chối hoặc bị chọn Không bao giờ
                        appSettingOpen(this)
                    } else {
                        // hiển thị cảnh báo cấp quyền
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun btnFlash_EventClickListener() {
        binding.btnFlash.setOnClickListener{
            setIconFlash(camera)
        }
    }

    private fun btnFlip_EventClickListener() {
        binding.btnFlipCamera.setOnClickListener {
            if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                lensFacing = CameraSelector.LENS_FACING_BACK
            else
                lensFacing = CameraSelector.LENS_FACING_FRONT
            bindCameraUseCases()
        }
    }

    private fun btnCapture_EventClickListener() {
        binding.btnTakePhoto.setOnClickListener{
            takePhoto()
        }
    }


    // xử lí khi người dùng ấn back
    override fun onBackPressed() {
        if (isBackClicked) {
            super.onBackPressed()
            return
        } // thoát ứng dụng

        isBackClicked = true
        StyleableToast.makeText(this@CameraActivity, "Nhấn back lần nữa để thoát", R.style.warning_toast).show()

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
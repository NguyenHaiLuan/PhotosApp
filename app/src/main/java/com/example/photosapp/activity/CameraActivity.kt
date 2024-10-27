package com.example.photosapp.activity

import android.Manifest
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
import android.os.Environment
import android.print.PrintAttributes.Resolution
import android.util.Log
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
import com.example.photosapp.utils.appSettingOpen
import com.example.photosapp.utils.warningPermissionDialog
import io.github.muddz.styleabletoast.StyleableToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var imageCapture : ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector : CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val multiplePermissionId = 609
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.CAMERA
        )
    } else {
        arrayListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

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
    }

    private fun takePhoto() {
        val imageFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images")

        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        val fileName = "IMG_"+SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpeg"
        val imageFile = File(imageFolder, fileName)

        val outOption = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(outOption, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                StyleableToast.makeText(this@CameraActivity, "${outputFileResults.savedUri}", R.style.success_toast).show()
            }

            override fun onError(exception: ImageCaptureException) {
                StyleableToast.makeText(this@CameraActivity, "Lỗi: ${exception.message}", R.style.error_toast).show()
            }
        })
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

    // xác định tỉ lệ khung hình phù hợp
    private fun aspectRatio(width:Int, height:Int):Int{
        val priviewRatio = maxOf(width, height).toDouble() / minOf(width,height)
        var result:Int

        if (abs(priviewRatio - 4.0/3.0) <= abs(priviewRatio - 16.0 / 9.0))
            result = AspectRatio.RATIO_4_3
        else
            result = AspectRatio.RATIO_16_9
        return result
    }

    private fun bindCameraUseCases() {
        val screenAspectRatio = aspectRatio(binding.viewFinder.width, binding.viewFinder.height)
        val rotation = getDisplayRotation()

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(screenAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
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

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
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

        if (requestCode == multiplePermissionId) {
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
        }
        bindCameraUseCases()
    }

    private fun btnCapture_EventClickListener() {
        binding.btnTakePhoto.setOnClickListener{
            takePhoto()
        }
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
}
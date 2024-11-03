package com.example.photosapp.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.photosapp.R
import com.example.photosapp.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    companion object{
        const val DELAY_MILLIS = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()

        binding.splashAnimationView.playAnimation()
        binding.splashAnimationView.postDelayed({
            val intent = Intent(this@SplashScreen, CameraActivity::class.java)
            startActivity(intent)
            finish()
        }, DELAY_MILLIS)
    }

    private fun initUI(){
        enableEdgeToEdge()
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
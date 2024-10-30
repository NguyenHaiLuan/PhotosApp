package com.example.photosapp.utils

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.example.photosapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.muddz.styleabletoast.StyleableToast

    fun appSettingOpen(context: Context){
        StyleableToast.makeText(context, "Vui lòng cung cấp tất cả các quyền cho ứng dụng ở phần cài đặt", R.style.warning_toast).show()

        val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        settingIntent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(settingIntent)
    }

    fun warningPermissionDialog(context: Context,listener : DialogInterface.OnClickListener){
        MaterialAlertDialogBuilder(context)
            .setMessage("Ứng dụng cần các quyền cần thiết để hoạt động! Vui lòng cung cấp đầy đủ tất cả các quyền!")
            .setCancelable(false)
            .setPositiveButton("Ok",listener)
            .create()
            .show()
    }

    fun View.visible(){
        visibility = View.VISIBLE
    }

    fun View.invisible(){
        visibility = View.INVISIBLE
    }
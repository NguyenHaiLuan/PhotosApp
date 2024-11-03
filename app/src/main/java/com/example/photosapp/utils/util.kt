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
        StyleableToast.makeText(context, context.getString(R.string.require_permission_message), R.style.warning_toast).show()

        val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        settingIntent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(settingIntent)
    }

    fun warningPermissionDialog(context: Context,listener : DialogInterface.OnClickListener){
        MaterialAlertDialogBuilder(context)
            .setMessage(context.getString(R.string.require_all_permission_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.ok_button_label),listener)
            .create()
            .show()
    }

    fun View.visible(){
        visibility = View.VISIBLE
    }

    fun View.invisible(){
        visibility = View.INVISIBLE
    }
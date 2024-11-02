package com.example.photosapp.dialog

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteMediaDialog {

    fun showDialog(context: Context, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Xóa media")
            .setMessage("Bạn có chắc chắn muốn xóa media này không?")
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Xóa") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .show()
    }
}
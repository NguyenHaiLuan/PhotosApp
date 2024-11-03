package com.example.photosapp.utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object FilterUtils {
    fun getNoneFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getBrighteningFilter(brightness: Float): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setScale(1f, 1f, 1f, 1f)
        val brightnessAdjustment = floatArrayOf(
            1f, 0f, 0f, 0f, brightness, // Red
            0f, 1f, 0f, 0f, brightness, // Green
            0f, 0f, 1f, 0f, brightness, // Blue
            0f, 0f, 0f, 1f, 0f         // Alpha
        )
        colorMatrix.postConcat(ColorMatrix(brightnessAdjustment))
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getContrastFilter(contrast: Float): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        val scale = contrast + 1f
        val translate = (-0.5f * scale + 0.5f) * 255

        val contrastAdjustment = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        colorMatrix.set(contrastAdjustment)
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getVibranceFilter(saturation: Float): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(saturation)
        return ColorMatrixColorFilter(colorMatrix)
    }


    fun getRedFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setScale(1f, 0f, 0f, 1f)
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getGreenFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setScale(0f, 1f, 0f, 1f)
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getBlueFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setScale(0f, 0f, 0.95f, 0.95f)
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getSepiaFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.set(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return ColorMatrixColorFilter(colorMatrix)
    }

    fun getGrayScaleFilter(): ColorMatrixColorFilter {
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // Đặt độ bão hòa thành 0 để chuyển sang màu xám
        return ColorMatrixColorFilter(colorMatrix)
    }
}
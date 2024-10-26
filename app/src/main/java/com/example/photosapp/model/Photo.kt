package com.example.photosapp.model

import android.net.Uri

data class Photo(
    val id: Long,
    val name: String,
    val uri: Uri
)

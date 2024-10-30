package com.example.photosapp.model

import android.net.Uri

data class Media(
    val id: Long,
    val name: String,
    val uri: Uri,
    val isVideo: Boolean = false,
    val dateAdded: Long,
    val duration: Long? = null
)

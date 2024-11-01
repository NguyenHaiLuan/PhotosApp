package com.example.photosapp.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Media(
    val id: Long,
    val name: String,
    val uriString: String,  // Lưu URI dưới dạng chuỗi
    val isVideo: Boolean = false,
    val dateAdded: Long,
    val duration: Long? = null
) : Parcelable {
    // Hàm để lấy Uri từ uriString
    val uri: Uri
        get() = Uri.parse(uriString)

    // Hàm tạo đối tượng Media từ Uri
    companion object {
        fun create(id: Long, name: String, uri: Uri, isVideo: Boolean = false, dateAdded: Long, duration: Long? = null): Media {
            return Media(id, name, uri.toString(), isVideo, dateAdded, duration)
        }
    }
}

package com.gps.camera.map.photo.gpscamera.features.map.repository

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gps.camera.map.photo.gpscamera.features.gallery.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoLocation(val uri: Uri, val latitude: Double, val longitude: Double)

class MapRepository(private val context: Context) {

    private val galleryRepository = GalleryRepository(context)

    suspend fun getPhotoLocations(): List<PhotoLocation> = withContext(Dispatchers.IO) {
        galleryRepository.getSavedPhotos().mapNotNull { uri ->
            readLocation(uri)?.let { (lat, lon) -> PhotoLocation(uri, lat, lon) }
        }
    }

    private fun readLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val latLong = ExifInterface(stream).latLong
                latLong?.let { it[0] to it[1] }
            }
        } catch (e: Exception) {
            null
        }
    }
}

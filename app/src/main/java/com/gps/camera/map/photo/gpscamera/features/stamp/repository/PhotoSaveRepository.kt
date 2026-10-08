package com.gps.camera.map.photo.gpscamera.features.stamp.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.gps.camera.map.photo.gpscamera.features.location.repository.AddressRepository
import com.gps.camera.map.photo.gpscamera.features.settings.data.StampPreferences
import com.gps.camera.map.photo.gpscamera.features.stamp.PhotoStampRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoSaveRepository(private val context: Context) {

    private val addressRepository = AddressRepository(context)
    private val preferences = StampPreferences(context)

    data class StampData(
        val addressLine: String?,
        val dateTimeLine: String?,
        val latLongLine: String?
    )

    suspend fun buildStampData(latitude: Double?, longitude: Double?): StampData {
        val addressLine = if (preferences.showAddress && latitude != null && longitude != null) {
            addressRepository.getAddressLine(latitude, longitude)
        } else {
            null
        }
        val dateTimeLine = buildDateTimeLine()
        val latLongLine = if (preferences.showLatLong && latitude != null && longitude != null) {
            "Lat: %.6f   Long: %.6f".format(latitude, longitude)
        } else {
            null
        }
        return StampData(addressLine, dateTimeLine, latLongLine)
    }

    fun applyStampToBitmap(
        photo: Bitmap,
        stampData: StampData,
        mapThumbnail: Bitmap?
    ): Bitmap = PhotoStampRenderer.applyStamp(
        source = photo,
        addressLine = stampData.addressLine,
        dateTimeLine = stampData.dateTimeLine,
        latLongLine = stampData.latLongLine,
        mapThumbnail = mapThumbnail,
        layoutStyle = preferences.stampLayout
    )

    private fun buildDateTimeLine(): String? {
        val showDate = preferences.showDate
        val showTime = preferences.showTime
        if (!showDate && !showTime) return null

        val timePattern = if (preferences.use24HourFormat) "HH:mm" else "hh:mm a"
        val pattern = when {
            showDate && showTime -> "dd MMM yyyy, $timePattern"
            showDate -> "dd MMM yyyy"
            else -> timePattern
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(System.currentTimeMillis())
    }

    /** Returns the saved photo's MediaStore Uri, or null on failure. */
    suspend fun saveToGallery(bitmap: Bitmap, latitude: Double?, longitude: Double?): Uri? =
        withContext(Dispatchers.IO) {
            val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "GPS_$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GPSCamera")
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                }
                if (latitude != null && longitude != null) {
                    writeGpsExif(uri, latitude, longitude)
                }
                uri
            } catch (e: Exception) {
                Log.e("GpsCamera", "Failed to write stamped photo", e)
                null
            }
        }

    private fun writeGpsExif(uri: android.net.Uri, latitude: Double, longitude: Double) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setLatLong(latitude, longitude)
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.e("GpsCamera", "Failed to write GPS EXIF data", e)
        }
    }
}

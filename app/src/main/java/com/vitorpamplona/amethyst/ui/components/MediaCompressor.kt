package com.vitorpamplona.amethyst.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaCompressor {
    suspend fun compress(
        uri: Uri,
        contentType: String?,
        applicationContext: Context,
        onReady: (Uri, String?, Long?) -> Unit,
        onError: (String) -> Unit
    ) {
        checkNotInMainThread()

        if (contentType?.startsWith("video", true) == true) {
            VideoCompressor.start(
                context = applicationContext, // => This is required
                uris = listOf(uri), // => Source can be provided as content uris
                isStreamable = false,
                // THIS STORAGE
                // sharedStorageConfiguration = SharedStorageConfiguration(
                //    saveAt = SaveLocation.movies, // => default is movies
                //    videoName = "compressed_video" // => required name
                // ),
                // OR AND NOT BOTH
                appSpecificStorageConfiguration = AppSpecificStorageConfiguration(),
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    videoNames = listOf(UUID.randomUUID().toString()) // => required name
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                    }

                    override fun onStart(index: Int) {
                        // Compression start
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        if (path != null) {
                            onReady(Uri.fromFile(File(path)), contentType, size)
                        } else {
                            onError("Compression Returned null")
                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        // keeps going with original video
                        onReady(uri, contentType, null)
                    }

                    override fun onCancelled(index: Int) {
                        onError("Compression Cancelled")
                    }
                }
            )
        } else if (contentType?.startsWith("image", true) == true && !contentType.contains("gif") && !contentType.contains("svg")) {
            try {
                val compressedImageFile = Compressor.compress(applicationContext, from(uri, contentType, applicationContext)) {
                    default(width = 640, format = Bitmap.CompressFormat.JPEG)
                }
                onReady(compressedImageFile.toUri(), contentType, compressedImageFile.length())
            } catch (e: Exception) {
                e.printStackTrace()
                onReady(uri, contentType, null)
            }
        } else {
            onReady(uri, contentType, null)
        }
    }

    fun from(uri: Uri?, contentType: String?, context: Context): File {
        val extension = contentType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: ""

        val inputStream = context.contentResolver.openInputStream(uri!!)
        val fileName: String = UUID.randomUUID().toString() + ".$extension"
        val splitName: Array<String> = splitFileName(fileName)
        val tempFile = File.createTempFile(splitName[0], splitName[1])
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(1024 * 50)
                var read: Int = input.read(buffer)
                while (read != -1) {
                    output.write(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
        }

        return tempFile
    }

    private fun splitFileName(fileName: String): Array<String> {
        var name = fileName
        var extension = ""
        val i = fileName.lastIndexOf(".")
        if (i != -1) {
            name = fileName.substring(0, i)
            extension = fileName.substring(i)
        }
        return arrayOf(name, extension)
    }
}

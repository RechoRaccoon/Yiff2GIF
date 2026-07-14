package rechoraccoon.yiff2gif.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object MediaSaver {
    private const val ALBUM = "Yiff2GIF"

    /** Saves the given gif bytes to the Pictures/Yiff2GIF album, returns true on success. */
    fun saveGif(context: Context, postId: Int, bytes: ByteArray): Boolean {
        val filename = "yiff2gif_$postId.gif"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$ALBUM")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    ALBUM
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { it.write(bytes) }
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    file.absolutePath,
                    filename,
                    "Yiff2GIF download"
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

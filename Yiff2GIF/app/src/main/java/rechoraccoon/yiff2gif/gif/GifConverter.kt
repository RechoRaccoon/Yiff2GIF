package rechoraccoon.yiff2gif.gif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import okhttp3.OkHttpClient
import okhttp3.Request
import rechoraccoon.yiff2gif.data.MediaKind
import rechoraccoon.yiff2gif.data.Post
import rechoraccoon.yiff2gif.data.mediaKind
import java.io.File

private val downloadClient = OkHttpClient.Builder().build()
private const val USER_AGENT = "Yiff2GIF/1.0 (by RechoRaccoon)"

private fun downloadBytes(url: String): ByteArray {
    val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
    downloadClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) throw IllegalStateException("Download failed: ${resp.code}")
        return resp.body?.bytes() ?: throw IllegalStateException("Empty body")
    }
}

private const val MAX_DIMENSION = 480
private const val MAX_FRAMES = 24

private fun scaleDown(bmp: Bitmap): Bitmap {
    val w = bmp.width
    val h = bmp.height
    val scale = MAX_DIMENSION.toFloat() / maxOf(w, h)
    if (scale >= 1f) return bmp
    return Bitmap.createScaledBitmap(bmp, (w * scale).toInt(), (h * scale).toInt(), true)
}

/**
 * Produces the final GIF byte array for a post, based on its media kind:
 *  - GIF: downloaded as-is (already a gif)
 *  - IMAGE: wrapped into a single-frame gif
 *  - VIDEO: frames sampled across the clip and encoded into an animated gif
 */
fun convertPostToGif(post: Post, cacheDir: File): ByteArray {
    val url = post.file?.url ?: throw IllegalStateException("Post has no file URL")
    return when (post.mediaKind()) {
        MediaKind.GIF -> downloadBytes(url)

        MediaKind.IMAGE -> {
            val bytes = downloadBytes(url)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalStateException("Could not decode image")
            encodeGif(listOf(scaleDown(bmp)), frameDelayMs = 0)
        }

        MediaKind.VIDEO -> {
            val bytes = downloadBytes(url)
            val tmp = File.createTempFile("yiff2gif_src", ".mp4", cacheDir)
            tmp.writeBytes(bytes)
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(tmp.absolutePath)
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 3000L

                val frameCount = MAX_FRAMES
                val stepUs = (durationMs * 1000) / frameCount
                val frames = mutableListOf<Bitmap>()
                for (i in 0 until frameCount) {
                    val timeUs = i * stepUs
                    val frame = retriever.getFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: continue
                    frames.add(scaleDown(frame))
                }
                retriever.release()
                if (frames.isEmpty()) throw IllegalStateException("No frames extracted")
                val perFrameDelayMs = (durationMs / frameCount).toInt().coerceAtLeast(20)
                encodeGif(frames, frameDelayMs = perFrameDelayMs)
            } finally {
                tmp.delete()
            }
        }

        MediaKind.UNKNOWN -> throw IllegalStateException("Unsupported file type")
    }
}

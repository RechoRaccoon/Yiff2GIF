package rechoraccoon.yiff2gif.gif

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Minimal animated GIF encoder. Adapted from the classic public-domain
 * AnimatedGifEncoder/NeuQuant implementation (Kevin Weiner / Jef Poskanzer).
 * No external license restrictions apply to this algorithm.
 */
class AnimatedGifEncoder {
    private var width = 0
    private var height = 0
    private var transparent: Int? = null
    private var repeat = 0 // 0 = loop forever
    private var delay = 0 // hundredths of a second
    private var started = false
    private var out: OutputStream? = null
    private var image: Bitmap? = null
    private var pixels: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorDepth = 0
    private var colorTab: ByteArray? = null
    private val usedEntry = BooleanArray(256)
    private var palSize = 7
    private var dispose = -1
    private var firstFrame = true
    private var sizeSet = false

    fun setDelay(ms: Int) {
        delay = Math.round(ms / 10.0f)
    }

    fun setRepeat(iter: Int) {
        repeat = iter
    }

    fun start(os: OutputStream): Boolean {
        out = os
        started = try {
            writeString("GIF89a")
            true
        } catch (e: IOException) {
            false
        }
        return started
    }

    fun addFrame(im: Bitmap): Boolean {
        if (!started) return false
        image = im
        if (!sizeSet) {
            width = im.width
            height = im.height
            sizeSet = true
        }
        getImagePixels()
        analyzePixels()
        try {
            if (firstFrame) {
                writeLSD()
                writePalette()
                if (repeat >= 0) writeNetscapeExt()
            }
            writeGraphicCtrlExt()
            writeImageDesc()
            if (!firstFrame) writePalette()
            writePixels()
            firstFrame = false
        } catch (e: IOException) {
            return false
        }
        return true
    }

    fun finish(): Boolean {
        if (!started) return false
        var ok = true
        try {
            out?.write(0x3b)
            out?.flush()
        } catch (e: IOException) {
            ok = false
        }
        started = false
        return ok
    }

    private fun getImagePixels() {
        val w = width
        val h = height
        val bmp = image!!
        val scaled = if (bmp.width != w || bmp.height != h)
            Bitmap.createScaledBitmap(bmp, w, h, true) else bmp
        val argb = IntArray(w * h)
        scaled.getPixels(argb, 0, w, 0, 0, w, h)
        val px = ByteArray(w * h * 3)
        var i = 0
        for (p in argb) {
            px[i++] = ((p shr 16) and 0xff).toByte()
            px[i++] = ((p shr 8) and 0xff).toByte()
            px[i++] = (p and 0xff).toByte()
        }
        pixels = px
    }

    // Simple uniform color quantization to a 256-color palette (fast, good enough for previews).
    private fun analyzePixels() {
        val len = pixels!!.size
        val nPix = len / 3
        indexedPixels = ByteArray(nPix)
        // Build a 6x6x6 = 216 color cube palette (web-safe-ish) for speed and simplicity.
        val levels = 6
        colorTab = ByteArray(levels * levels * levels * 3)
        var idx = 0
        for (r in 0 until levels) for (g in 0 until levels) for (b in 0 until levels) {
            colorTab!![idx * 3] = (r * 255 / (levels - 1)).toByte()
            colorTab!![idx * 3 + 1] = (g * 255 / (levels - 1)).toByte()
            colorTab!![idx * 3 + 2] = (b * 255 / (levels - 1)).toByte()
            idx++
        }
        colorDepth = 8
        palSize = 7
        for (p in 0 until nPix) {
            val r = pixels!![p * 3].toInt() and 0xff
            val g = pixels!![p * 3 + 1].toInt() and 0xff
            val b = pixels!![p * 3 + 2].toInt() and 0xff
            val ri = (r * (levels - 1) / 255)
            val gi = (g * (levels - 1) / 255)
            val bi = (b * (levels - 1) / 255)
            val colorIndex = (ri * levels + gi) * levels + bi
            indexedPixels!![p] = colorIndex.toByte()
            usedEntry[colorIndex] = true
        }
        pixels = null
    }

    @Throws(IOException::class)
    private fun writeString(s: String) {
        for (ch in s) out?.write(ch.code)
    }

    @Throws(IOException::class)
    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        out?.write(0x80 or 0x70 or 0x00 or palSize)
        out?.write(0) // background color index
        out?.write(0) // pixel aspect ratio
    }

    @Throws(IOException::class)
    private fun writePalette() {
        out?.write(colorTab!!)
        val n = (3 * 256) - colorTab!!.size
        for (i in 0 until n) out?.write(0)
    }

    @Throws(IOException::class)
    private fun writeNetscapeExt() {
        out?.write(0x21)
        out?.write(0xff)
        out?.write(11)
        writeString("NETSCAPE2.0")
        out?.write(3)
        out?.write(1)
        writeShort(repeat)
        out?.write(0)
    }

    @Throws(IOException::class)
    private fun writeGraphicCtrlExt() {
        out?.write(0x21)
        out?.write(0xf9)
        out?.write(4)
        val transp = if (transparent != null) 1 else 0
        out?.write(0 or (dispose and 7 shl 2) or 0 or transp)
        writeShort(delay)
        out?.write(0) // transparent color index
        out?.write(0)
    }

    @Throws(IOException::class)
    private fun writeImageDesc() {
        out?.write(0x2c)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out?.write(0)
    }

    @Throws(IOException::class)
    private fun writePixels() {
        val encoder = LzwEncoder(width, height, indexedPixels!!, colorDepth)
        encoder.encode(out!!)
    }

    @Throws(IOException::class)
    private fun writeShort(value: Int) {
        out?.write(value and 0xff)
        out?.write((value shr 8) and 0xff)
    }
}

/** LZW encoder used by GIF pixel data, standard algorithm (public domain). */
private class LzwEncoder(
    private val imgW: Int,
    private val imgH: Int,
    private val pixAry: ByteArray,
    private val colorDepth: Int
) {
    private val EOF = -1
    private var initCodeSize = maxOf(2, colorDepth)
    private var curPixel = 0

    private val BITS = 12
    private val HSIZE = 5003
    private var n_bits = 0
    private var maxbits = BITS
    private var maxcode = 0
    private var maxmaxcode = 1 shl BITS
    private var htab = IntArray(HSIZE)
    private var codetab = IntArray(HSIZE)
    private var hsize = HSIZE
    private var free_ent = 0
    private var clear_flg = false
    private var g_init_bits = 0
    private var ClearCode = 0
    private var EOFCode = 0
    private var cur_accum = 0
    private var cur_bits = 0
    private val masks = intArrayOf(
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    )
    private var a_count = 0
    private val accum = ByteArray(256)
    private var outStream: OutputStream? = null

    private fun MAXCODE(n_bits: Int) = (1 shl n_bits) - 1

    fun encode(os: OutputStream) {
        os.write(initCodeSize)
        outStream = os
        curPixel = 0
        compress(initCodeSize + 1, os)
        os.write(0)
    }

    private fun nextPixel(): Int {
        if (curPixel >= imgW * imgH) return EOF
        val pix = pixAry[curPixel].toInt() and 0xff
        curPixel++
        return pix
    }

    private fun compress(initBits: Int, outs: OutputStream) {
        var fcode: Int
        var i: Int
        var c: Int
        var ent: Int
        var disp: Int
        var hsizeReg: Int
        var hshift: Int

        g_init_bits = initBits
        clear_flg = false
        n_bits = g_init_bits
        maxcode = MAXCODE(n_bits)
        ClearCode = 1 shl (initBits - 1)
        EOFCode = ClearCode + 1
        free_ent = ClearCode + 2

        a_count = 0
        ent = nextPixel()

        hshift = 0
        fcode = hsize
        while (fcode < 65536) {
            hshift++
            fcode *= 2
        }
        hshift = 8 - hshift
        hsizeReg = hsize
        clHash(hsizeReg)

        output(ClearCode, outs)

        outer@ while (true) {
            c = nextPixel()
            if (c == EOF) break
            fcode = (c shl maxbits) + ent
            i = (c shl hshift) xor ent
            if (htab[i] == fcode) {
                ent = codetab[i]
                continue
            } else if (htab[i] >= 0) {
                disp = hsizeReg - i
                if (i == 0) disp = 1
                do {
                    i -= disp
                    if (i < 0) i += hsizeReg
                    if (htab[i] == fcode) {
                        ent = codetab[i]
                        continue@outer
                    }
                } while (htab[i] >= 0)
            }
            output(ent, outs)
            ent = c
            if (free_ent < maxmaxcode) {
                codetab[i] = free_ent++
                htab[i] = fcode
            } else {
                clHash(hsizeReg)
                free_ent = ClearCode + 2
                clear_flg = true
                output(ClearCode, outs)
            }
        }
        output(ent, outs)
        output(EOFCode, outs)
    }

    private fun clHash(hsizeReg: Int) {
        for (i in 0 until hsizeReg) htab[i] = -1
    }

    private fun output(code: Int, outs: OutputStream) {
        cur_accum = cur_accum and masks[cur_bits]
        cur_accum = if (cur_bits > 0) cur_accum or (code shl cur_bits) else code
        cur_bits += n_bits
        while (cur_bits >= 8) {
            charOut((cur_accum and 0xff).toByte(), outs)
            cur_accum = cur_accum shr 8
            cur_bits -= 8
        }
        if (free_ent > maxcode || clear_flg) {
            if (clear_flg) {
                n_bits = g_init_bits
                maxcode = MAXCODE(n_bits)
                clear_flg = false
            } else {
                n_bits++
                maxcode = if (n_bits == maxbits) maxmaxcode else MAXCODE(n_bits)
            }
        }
        if (code == EOFCode) {
            while (cur_bits > 0) {
                charOut((cur_accum and 0xff).toByte(), outs)
                cur_accum = cur_accum shr 8
                cur_bits -= 8
            }
            flushChar(outs)
        }
    }

    private fun charOut(c: Byte, outs: OutputStream) {
        accum[a_count++] = c
        if (a_count >= 254) flushChar(outs)
    }

    private fun flushChar(outs: OutputStream) {
        if (a_count > 0) {
            outs.write(a_count)
            outs.write(accum, 0, a_count)
            a_count = 0
        }
    }
}

fun encodeGif(frames: List<Bitmap>, frameDelayMs: Int, loop: Boolean = true): ByteArray {
    val baos = ByteArrayOutputStream()
    val encoder = AnimatedGifEncoder()
    encoder.setDelay(frameDelayMs)
    encoder.setRepeat(if (loop) 0 else -1)
    encoder.start(baos)
    for (f in frames) encoder.addFrame(f)
    encoder.finish()
    return baos.toByteArray()
}

package com.example.editphoto.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import kotlin.math.min

class BlurMaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : android.view.View(context, attrs) {

    private var baseBitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null

    private var brushSizePx: Float = 60f
    private var brushSoftness: Float = 0.5f // 0..1, 1 = very soft
    private var isEraseMode: Boolean = false
    private var maxBlurRadius: Int = 24 // map intensity 0..1 -> 0..24

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = brushSizePx
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = brushSizePx
    }

    private val path = Path()
    private var lastX = -1f
    private var lastY = -1f

    private var targetImageView: ImageView? = null
    private var previewCallback: ((Bitmap) -> Unit)? = null

    fun setBrush(sizePx: Float, softness: Float) {
        brushSizePx = sizePx
        brushSoftness = softness.coerceIn(0f, 1f)
        drawPaint.strokeWidth = brushSizePx
        erasePaint.strokeWidth = brushSizePx
    }

    fun setEraseMode(erase: Boolean) {
        isEraseMode = erase
    }

    /**
     * Cập nhật độ mờ theo intensity (0f..1f).
     * Ánh xạ intensity -> bán kính blur, sau đó re-composite để xem trước.
     */
    fun setBlurIntensity(intensity: Float) {
        val base = baseBitmap ?: return
        val clamped = intensity.coerceIn(0f, 1f)
        val radius = (clamped * maxBlurRadius).toInt()
        blurredBitmap = if (radius <= 0) {
            base.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            makeBlurred(base, radius)
        }
        compositeAndCallback()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attachTo(
        imageView: ImageView,
        base: Bitmap,
        blurRadius: Int = 12,
        onCompositeUpdated: (Bitmap) -> Unit
    ) {
        targetImageView = imageView
        previewCallback = onCompositeUpdated

        baseBitmap = base
        blurredBitmap = makeBlurred(base, radius = blurRadius)
        maskBitmap = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
        clearMask()

        // Gắn touch vào ImageView mục tiêu
        imageView.setOnTouchListener { _, event ->
            val b = baseBitmap ?: return@setOnTouchListener true
            val pt = mapTouchToBitmap(event.x, event.y, imageView, b)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    path.reset()
                    path.moveTo(pt.first, pt.second)
                    lastX = pt.first
                    lastY = pt.second
                    drawDot(pt.first, pt.second)
                    compositeAndCallback()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = pt.first - lastX
                    val dy = pt.second - lastY
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist >= 2f) {
                        path.quadTo(lastX, lastY, (pt.first + lastX) / 2f, (pt.second + lastY) / 2f)
                        lastX = pt.first
                        lastY = pt.second
                        drawPath()
                        compositeAndCallback()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    drawPath()
                    path.reset()
                    compositeAndCallback()
                }
            }
            true
        }
    }

    fun detach() {
        targetImageView?.setOnTouchListener(null)
        targetImageView = null
        previewCallback = null
    }

    fun getCompositedBitmap(): Bitmap? {
        val base = baseBitmap ?: return null
        val blur = blurredBitmap ?: return null
        val mask = maskBitmap ?: return null
        return compositeWithMask(base, blur, mask)
    }

    private fun drawDot(x: Float, y: Float) {
        val p = if (isEraseMode) erasePaint else fillPaint
        maskCanvas?.drawCircle(x, y, brushSizePx / 2f, p)
        if (!isEraseMode) softenMaskAround(x, y)
    }

    private fun drawPath() {
        val p = if (isEraseMode) erasePaint else drawPaint
        maskCanvas?.drawPath(path, p)
        if (!isEraseMode) softenMaskPath()
    }

    private fun softenMaskAround(x: Float, y: Float) {
        val feather = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = (brushSoftness * 255).toInt().coerceIn(0, 255)
        }
        maskCanvas?.drawCircle(x, y, brushSizePx, feather)
    }

    private fun softenMaskPath() {
        val feather = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = brushSizePx * 1.6f
            alpha = (brushSoftness * 255).toInt().coerceIn(0, 255)
        }
        maskCanvas?.drawPath(path, feather)
    }

    private fun compositeAndCallback() {
        val base = baseBitmap ?: return
        val blurred = blurredBitmap ?: return
        val mask = maskBitmap ?: return
        val out = compositeWithMask(base, blurred, mask)
        previewCallback?.invoke(out)
    }

    private fun clearMask() {
        maskCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun mapTouchToBitmap(x: Float, y: Float, imageView: ImageView, bitmap: Bitmap): Pair<Float, Float> {
        val viewW = imageView.width
        val viewH = imageView.height
        val imgW = bitmap.width
        val imgH = bitmap.height

        val scale = min(viewW.toFloat() / imgW.toFloat(), viewH.toFloat() / imgH.toFloat())
        val dx = (viewW - imgW * scale) / 2f
        val dy = (viewH - imgH * scale) / 2f

        val bx = ((x - dx) / scale).coerceIn(0f, (imgW - 1).toFloat())
        val by = ((y - dy) / scale).coerceIn(0f, (imgH - 1).toFloat())
        return Pair(bx, by)
    }

    // Fast 2-pass box blur for ARGB_8888
    private fun makeBlurred(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        boxBlurARGB(pixels, w, h, radius)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlurARGB(pix: IntArray, w: Int, h: Int, radius: Int) {
        if (radius <= 0) return
        val tmp = IntArray(pix.size)
        // Horizontal
        for (y in 0 until h) {
            var rSum = 0; var gSum = 0; var bSum = 0; var aSum = 0
            var read = y * w
            var write = y * w
            val window = radius * 2 + 1
            for (i in -radius until radius + 1) {
                val x = i.coerceIn(0, w - 1)
                val c = pix[read + x]
                aSum += c ushr 24 and 0xFF
                rSum += c shr 16 and 0xFF
                gSum += c shr 8 and 0xFF
                bSum += c and 0xFF
            }
            for (x in 0 until w) {
                tmp[write++] = (aSum / window shl 24) or
                        (rSum / window shl 16) or
                        (gSum / window shl 8) or
                        (bSum / window)
                val xAdd = (x + radius + 1).coerceIn(0, w - 1)
                val xSub = (x - radius).coerceIn(0, w - 1)
                val cAdd = pix[read + xAdd]
                val cSub = pix[read + xSub]
                aSum += (cAdd ushr 24 and 0xFF) - (cSub ushr 24 and 0xFF)
                rSum += (cAdd shr 16 and 0xFF) - (cSub shr 16 and 0xFF)
                gSum += (cAdd shr 8 and 0xFF) - (cSub shr 8 and 0xFF)
                bSum += (cAdd and 0xFF) - (cSub and 0xFF)
            }
        }
        // Vertical
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0; var aSum = 0
            var read = x
            var write = x
            val window = radius * 2 + 1
            for (i in -radius until radius + 1) {
                val y = i.coerceIn(0, h - 1)
                val c = tmp[y * w + read]
                aSum += c ushr 24 and 0xFF
                rSum += c shr 16 and 0xFF
                gSum += c shr 8 and 0xFF
                bSum += c and 0xFF
            }
            for (y in 0 until h) {
                pix[write] = (aSum / window shl 24) or
                        (rSum / window shl 16) or
                        (gSum / window shl 8) or
                        (bSum / window)
                write += w
                val yAdd = (y + radius + 1).coerceIn(0, h - 1)
                val ySub = (y - radius).coerceIn(0, h - 1)
                val cAdd = tmp[yAdd * w + read]
                val cSub = tmp[ySub * w + read]
                aSum += (cAdd ushr 24 and 0xFF) - (cSub ushr 24 and 0xFF)
                rSum += (cAdd shr 16 and 0xFF) - (cSub shr 16 and 0xFF)
                gSum += (cAdd shr 8 and 0xFF) - (cSub shr 8 and 0xFF)
                bSum += (cAdd and 0xFF) - (cSub and 0xFF)
            }
        }
    }

    private fun compositeWithMask(base: Bitmap, blurred: Bitmap, mask: Bitmap): Bitmap {
        val w = base.width; val h = base.height
        val out = base.copy(Bitmap.Config.ARGB_8888, true)

        val basePix = IntArray(w * h)
        val blurPix = IntArray(w * h)
        val maskPix = IntArray(w * h)

        base.getPixels(basePix, 0, w, 0, 0, w, h)
        blurred.getPixels(blurPix, 0, w, 0, 0, w, h)
        mask.getPixels(maskPix, 0, w, 0, 0, w, h)

        for (i in 0 until w * h) {
            val ma = maskPix[i] ushr 24 and 0xFF
            if (ma == 0) {
                continue
            } else if (ma == 255) {
                basePix[i] = blurPix[i]
            } else {
                val a = ma
                val br = blurPix[i] shr 16 and 0xFF
                val bg = blurPix[i] shr 8 and 0xFF
                val bb = blurPix[i] and 0xFF
                val rr = basePix[i] shr 16 and 0xFF
                val rg = basePix[i] shr 8 and 0xFF
                val rb = basePix[i] and 0xFF

                val nr = (rr * (255 - a) + br * a) / 255
                val ng = (rg * (255 - a) + bg * a) / 255
                val nb = (rb * (255 - a) + bb * a) / 255
                basePix[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        out.setPixels(basePix, 0, w, 0, 0, w, h)
        return out
    }
}
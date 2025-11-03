package com.example.editphoto.view


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BlurDrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var originalBitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private var maskBitmap: Bitmap? = null
    private var maskCanvas: Canvas? = null

    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 100f
        strokeCap = Paint.Cap.ROUND
    }

    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = 100f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    enum class Mode { BLUR, ERASE, NONE }

    var mode: Mode = Mode.NONE

    fun setImage(bitmap: Bitmap) {
        originalBitmap = bitmap
        blurredBitmap = createBlurBitmap(bitmap)
        maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val orig = originalBitmap ?: return
        val blur = blurredBitmap ?: return
        val mask = maskBitmap ?: return

        // Vẽ ảnh gốc trước
        canvas.drawBitmap(orig, 0f, 0f, null)

        // Tạo bitmap vùng mờ (blur + mask)
        val maskedBlur = Bitmap.createBitmap(blur.width, blur.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(maskedBlur)
        c.drawBitmap(blur, 0f, 0f, null)
        c.drawBitmap(mask, 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })

        // Vẽ phần mờ lên trên ảnh gốc
        canvas.drawBitmap(maskedBlur, 0f, 0f, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    Mode.BLUR -> maskCanvas?.drawCircle(x, y, drawPaint.strokeWidth / 2, drawPaint)
                    Mode.ERASE -> maskCanvas?.drawCircle(
                        x,
                        y,
                        erasePaint.strokeWidth / 2,
                        erasePaint
                    )

                    else -> {}
                }
                invalidate()
            }
        }
        return true
    }

    private fun createBlurBitmap(src: Bitmap): Bitmap {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // 👉 API 31+ : dùng RenderEffect
            val blurBitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(blurBitmap)
            val paint = Paint()

            val effect = android.graphics.RenderEffect.createBlurEffect(
                25f, 25f, android.graphics.Shader.TileMode.CLAMP
            )
            // LƯU Ý: cần import đúng RenderEffect
            paint.setRenderEffect(effect)
            canvas.drawBitmap(src, 0f, 0f, paint)
            blurBitmap
        } else {
            // 👉 API < 31 : fallback RenderScript
            val out = src.copy(src.config, true)
            try {
                val rs = android.renderscript.RenderScript.create(context)
                val input = android.renderscript.Allocation.createFromBitmap(rs, src)
                val output = android.renderscript.Allocation.createTyped(rs, input.type)
                val blur = android.renderscript.ScriptIntrinsicBlur.create(
                    rs,
                    android.renderscript.Element.U8_4(rs)
                )
                blur.setRadius(25f)
                blur.setInput(input)
                blur.forEach(output)
                output.copyTo(out)
                rs.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            out
        }
    }


    fun getFinalBitmap(): Bitmap? {
        val orig = originalBitmap ?: return null
        val blur = blurredBitmap ?: return null
        val mask = maskBitmap ?: return null

        // Tạo bitmap kết quả có kích thước ảnh gốc
        val result = Bitmap.createBitmap(orig.width, orig.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Vẽ ảnh gốc
        canvas.drawBitmap(orig, 0f, 0f, null)

        // Tạo lớp mờ chỉ trong vùng mask
        val maskedBlur = Bitmap.createBitmap(blur.width, blur.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(maskedBlur)
        c.drawBitmap(blur, 0f, 0f, null)
        c.drawBitmap(mask, 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })

        // Vẽ lớp mờ này đè lên ảnh gốc
        canvas.drawBitmap(maskedBlur, 0f, 0f, null)

        return result
    }

}

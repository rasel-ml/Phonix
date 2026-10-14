package app.mrm.phonix.lib.ui.contacteditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shows a photo under a square crop area. The photo can be moved with one finger and zoomed with a
 * pinch; the square can be resized by dragging its corners. A circle inside the square shows what
 * will be visible once the photo is used as a round contact picture.
 */
class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private enum class Mode { NONE, MOVE, RESIZE, PINCH }

    private val density = resources.displayMetrics.density
    private val margin = 24 * density
    private val edge = 8 * density
    private val handleTouchRadius = 40 * density
    private val handleLength = 20 * density
    private val minSide = 96 * density

    private var bitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private var scale = 1f
    private var baseScale = 1f
    private var tx = 0f
    private var ty = 0f
    private val crop = RectF()

    private var mode = Mode.NONE
    private var activeCorner = -1
    private var lastX = 0f
    private var lastY = 0f

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val outsidePaint = Paint().apply { color = 0xB8000000.toInt() }
    private val betweenPaint = Paint().apply { color = 0x70000000 }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4 * density
        strokeCap = Paint.Cap.SQUARE
    }
    private val outsidePath = Path()
    private val betweenPath = Path()

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val target = max(min(scale * detector.scaleFactor, maxScale()), minScale())
                val factor = target / scale
                tx = detector.focusX - (detector.focusX - tx) * factor
                ty = detector.focusY - (detector.focusY - ty) * factor
                scale = target
                clampImage()
                invalidate()
                return true
            }
        })


    fun setBitmap(newBitmap: Bitmap) {
        bitmap = newBitmap
        resetCrop()
    }

    /** the part of the photo inside the square, at most [outSize] px wide */
    fun getCroppedBitmap(outSize: Int): Bitmap? {
        val bmp = bitmap ?: return null
        val x = ((crop.left - tx) / scale).roundToInt().coerceIn(0, bmp.width - 1)
        val y = ((crop.top - ty) / scale).roundToInt().coerceIn(0, bmp.height - 1)
        val side = (crop.width() / scale).roundToInt().coerceAtLeast(1)
        val size = min(side, min(bmp.width - x, bmp.height - y)).coerceAtLeast(1)
        val region = Bitmap.createBitmap(bmp, x, y, size, size)
        return if (size > outSize) Bitmap.createScaledBitmap(region, outSize, outSize, true) else region
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetCrop()
    }

    private fun resetCrop() {
        val bmp = bitmap ?: return
        if (width == 0 || height == 0) return

        val side = max(min(width, height) - 2 * margin, minSide)
        val left = (width - side) / 2
        val top = (height - side) / 2
        crop.set(left, top, left + side, top + side)

        baseScale = max(crop.width() / bmp.width, crop.height() / bmp.height)
        scale = baseScale
        tx = (width - bmp.width * scale) / 2
        ty = (height - bmp.height * scale) / 2
        clampImage()
        invalidate()
    }

    private fun minScale(): Float {
        val bmp = bitmap ?: return 1f
        return max(crop.width() / bmp.width, crop.height() / bmp.height)
    }

    private fun maxScale() = max(baseScale * 6f, minScale())

    /** keeps the photo covering the whole crop square */
    private fun clampImage() {
        val bmp = bitmap ?: return
        scale = max(min(scale, maxScale()), minScale())
        val w = bmp.width * scale
        val h = bmp.height * scale
        tx = max(min(tx, crop.left), crop.right - w)
        ty = max(min(ty, crop.top), crop.bottom - h)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap ?: return

        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(tx, ty)
        canvas.drawBitmap(bmp, drawMatrix, bitmapPaint)

        // everything outside the square
        outsidePath.reset()
        outsidePath.fillType = Path.FillType.EVEN_ODD
        outsidePath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        outsidePath.addRect(crop, Path.Direction.CW)
        canvas.drawPath(outsidePath, outsidePaint)

        // between the square and the circle
        val radius = crop.width() / 2
        betweenPath.reset()
        betweenPath.fillType = Path.FillType.EVEN_ODD
        betweenPath.addRect(crop, Path.Direction.CW)
        betweenPath.addCircle(crop.centerX(), crop.centerY(), radius, Path.Direction.CW)
        canvas.drawPath(betweenPath, betweenPaint)

        canvas.drawRect(crop, borderPaint)
        canvas.drawCircle(crop.centerX(), crop.centerY(), radius, circlePaint)

        // corner handles
        val l = crop.left
        val t = crop.top
        val r = crop.right
        val b = crop.bottom
        val len = handleLength
        canvas.drawLine(l, t, l + len, t, handlePaint)
        canvas.drawLine(l, t, l, t + len, handlePaint)
        canvas.drawLine(r, t, r - len, t, handlePaint)
        canvas.drawLine(r, t, r, t + len, handlePaint)
        canvas.drawLine(l, b, l + len, b, handlePaint)
        canvas.drawLine(l, b, l, b - len, handlePaint)
        canvas.drawLine(r, b, r - len, b, handlePaint)
        canvas.drawLine(r, b, r, b - len, handlePaint)
    }

    // region touch

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = event.x
                lastY = event.y
                activeCorner = cornerAt(event.x, event.y)
                mode = if (activeCorner >= 0) Mode.RESIZE else Mode.MOVE
            }
            MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.PINCH
            MotionEvent.ACTION_MOVE -> when (mode) {
                Mode.MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        tx += event.x - lastX
                        ty += event.y - lastY
                        clampImage()
                        invalidate()
                    }
                    lastX = event.x
                    lastY = event.y
                }
                Mode.RESIZE -> resize(event.x, event.y)
                else -> Unit
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = if (event.actionIndex == 0) 1 else 0
                lastX = event.getX(remaining)
                lastY = event.getY(remaining)
                mode = if (event.pointerCount - 1 <= 1) Mode.MOVE else Mode.PINCH
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mode = Mode.NONE
        }
        return true
    }

    /** 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right, -1 = none */
    private fun cornerAt(x: Float, y: Float): Int {
        val corners = arrayOf(
            crop.left to crop.top, crop.right to crop.top,
            crop.left to crop.bottom, crop.right to crop.bottom
        )
        var best = -1
        var bestDistance = handleTouchRadius
        corners.forEachIndexed { index, (cx, cy) ->
            val distance = hypot(x - cx, y - cy)
            if (distance <= bestDistance) {
                best = index
                bestDistance = distance
            }
        }
        return best
    }

    private fun resize(x: Float, y: Float) {
        // the corner opposite to the dragged one stays where it is
        val anchorX = if (activeCorner == 0 || activeCorner == 2) crop.right else crop.left
        val anchorY = if (activeCorner == 0 || activeCorner == 1) crop.bottom else crop.top
        val dirX = if (activeCorner == 0 || activeCorner == 2) -1 else 1
        val dirY = if (activeCorner == 0 || activeCorner == 1) -1 else 1

        var side = max((x - anchorX) * dirX, (y - anchorY) * dirY)
        val availableX = if (dirX > 0) width - edge - anchorX else anchorX - edge
        val availableY = if (dirY > 0) height - edge - anchorY else anchorY - edge
        side = max(min(side, min(availableX, availableY)), minSide)

        val left = if (dirX > 0) anchorX else anchorX - side
        val top = if (dirY > 0) anchorY else anchorY - side
        crop.set(left, top, left + side, top + side)
        clampImage()
        invalidate()
    }

    // endregion
}

package com.example.pinpoint.admin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropRotateImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sourceBitmap: Bitmap? = null
    private val imageMatrix = Matrix()

    private var baseScale = 1f
    private var userScale = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var rotationDegrees = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A60B1C30")
        style = Paint.Style.FILL
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    var onZoomChanged: ((Float) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            userScale = (userScale * detector.scaleFactor).coerceIn(0.5f, 4.0f)
            onZoomChanged?.invoke(userScale)
            updateMatrix()
            invalidate()
            return true
        }
    })

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        sourceBitmap = bitmap
        resetTransform()
    }

    fun resetTransform() {
        userScale = 1f
        translateX = 0f
        translateY = 0f
        rotationDegrees = 0f
        calculateBaseScale()
        updateMatrix()
        invalidate()
    }

    fun rotateBy(deltaDegrees: Float) {
        rotationDegrees = (rotationDegrees + deltaDegrees) % 360f
        if (rotationDegrees > 180f) rotationDegrees -= 360f
        if (rotationDegrees < -180f) rotationDegrees += 360f
        updateMatrix()
        invalidate()
    }

    fun setRotationAngle(degrees: Float) {
        rotationDegrees = degrees.coerceIn(-180f, 180f)
        updateMatrix()
        invalidate()
    }

    fun getRotationAngle(): Float = rotationDegrees

    fun setZoomRatio(zoom: Float) {
        userScale = zoom.coerceIn(0.5f, 4.0f)
        updateMatrix()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBaseScale()
        updateMatrix()
    }

    private fun getCropRadius(): Float {
        val minDim = min(width, height).toFloat()
        return (minDim * 0.40f).coerceAtLeast(60f)
    }

    private fun calculateBaseScale() {
        val bmp = sourceBitmap ?: return
        if (width == 0 || height == 0) return
        val cropDiameter = getCropRadius() * 2f
        val scaleX = cropDiameter / bmp.width.toFloat()
        val scaleY = cropDiameter / bmp.height.toFloat()
        baseScale = max(scaleX, scaleY)
    }

    private fun updateMatrix() {
        val bmp = sourceBitmap ?: return
        if (width == 0 || height == 0) return
        if (baseScale == 1f) calculateBaseScale()

        val cx = width / 2f
        val cy = height / 2f
        val totalScale = baseScale * userScale

        imageMatrix.reset()
        imageMatrix.postTranslate(-bmp.width / 2f, -bmp.height / 2f)
        imageMatrix.postScale(totalScale, totalScale)
        imageMatrix.postRotate(rotationDegrees)
        imageMatrix.postTranslate(cx + translateX, cy + translateY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0B1C30"))

        val bmp = sourceBitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, imageMatrix, bitmapPaint)
        }

        val cx = width / 2f
        val cy = height / 2f
        val radius = getCropRadius()

        // Gambar layer gelap di luar lingkaran crop
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawCircle(cx, cy, radius, clearPaint)
        canvas.restoreToCount(saveCount)

        // Garis bantu grid 3x3 di dalam lingkaran crop
        val left = cx - radius
        val top = cy - radius
        val right = cx + radius
        val bottom = cy + radius
        val step = (radius * 2f) / 3f

        val clipSave = canvas.save()
        val circlePath = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        canvas.clipPath(circlePath)
        canvas.drawLine(left + step, top, left + step, bottom, gridPaint)
        canvas.drawLine(left + step * 2, top, left + step * 2, bottom, gridPaint)
        canvas.drawLine(left, top + step, right, top + step, gridPaint)
        canvas.drawLine(left, top + step * 2, right, top + step * 2, gridPaint)
        canvas.restoreToCount(clipSave)

        // Lingkaran border putih
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        translateX += (x - lastTouchX)
                        translateY += (y - lastTouchY)
                        lastTouchX = x
                        lastTouchY = y
                        updateMatrix()
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    fun getCroppedBitmap(outputSize: Int = 512): Bitmap? {
        val bmp = sourceBitmap ?: return null
        val radius = getCropRadius()
        val cropDiameter = radius * 2f
        val cx = width / 2f
        val cy = height / 2f
        val cropLeft = cx - radius
        val cropTop = cy - radius

        val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val exportMatrix = Matrix(imageMatrix)
        // Geser sehingga kiri-atas area crop berada di (0,0) lalu skalakan ke outputSize
        exportMatrix.postTranslate(-cropLeft, -cropTop)
        val scaleFactor = outputSize.toFloat() / cropDiameter
        exportMatrix.postScale(scaleFactor, scaleFactor)

        canvas.drawBitmap(bmp, exportMatrix, bitmapPaint)
        return result
    }
}


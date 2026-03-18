package com.example.qrverificationapp.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#99000000") // darker outside area
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var boxRect: RectF? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthF = width.toFloat()
        val heightF = height.toFloat()

        val boxSize = widthF * 0.65f

        val left = (widthF - boxSize) / 2
        val top = (heightF - boxSize) / 2
        val right = left + boxSize
        val bottom = top + boxSize

        boxRect = RectF(left, top, right, bottom)

        // Draw dark background
        canvas.drawRect(0f, 0f, widthF, heightF, backgroundPaint)

        // Clear center scanning area
        boxRect?.let {
            canvas.drawRect(it, clearPaint)
            canvas.drawRect(it, boxPaint) // white border
        }
    }

    fun getBoxRect(): RectF? = boxRect
}
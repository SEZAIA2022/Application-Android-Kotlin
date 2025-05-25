package com.houssein.sezaia.ui.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarcodeOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var rect: RectF? = null
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    fun setBoundingBox(boundingBox: RectF) {
        this.rect = boundingBox
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect?.let {
            canvas.drawRect(it, paint)
        }
    }

    fun clearBox() {
        rect = null
        invalidate()
    }
}

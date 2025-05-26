package com.houssein.sezaia.ui.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarcodeOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var boundingBox: RectF? = null

    // Peinture du cadre
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00E676") // vert lumineux
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        setShadowLayer(25f, 0f, 0f, Color.GREEN) // effet glow
    }

    // Peinture pour masquer l’arrière-plan
    private val maskPaint = Paint().apply {
        color = Color.parseColor("#AA000000") // noir semi-transparent
    }

    private val cornerRadius = 30f

    init {
        // Nécessaire pour les effets de glow sur Android < 9
        setLayerType(LAYER_TYPE_SOFTWARE, borderPaint)
    }

    fun setBoundingBox(box: RectF) {
        boundingBox = box
        invalidate()
    }

    fun clearBox() {
        boundingBox = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        boundingBox?.let { box ->
            // Masquer tout sauf la zone du QR code (trou découpé dans un masque)
            val path = Path().apply {
                addRect(0f, 0f, width, height, Path.Direction.CW)
                addRoundRect(box, cornerRadius, cornerRadius, Path.Direction.CCW)
            }
            canvas.drawPath(path, maskPaint)

            // Dessiner le cadre arrondi autour du QR code
            canvas.drawRoundRect(box, cornerRadius, cornerRadius, borderPaint)
        }
    }
}

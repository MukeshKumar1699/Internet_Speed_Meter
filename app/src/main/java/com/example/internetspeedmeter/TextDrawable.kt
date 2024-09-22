package com.example.internetspeedmeter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class TextDrawable(private val text: String) : Drawable() {
    private val paint: Paint = Paint().apply {
        color = Color.BLACK // Set text color
        textSize = 50f // Set text size
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        // Measure text to center it
        val textWidth = paint.measureText(text)
        val x = (bounds.width() - textWidth) / 2
        val y = (bounds.height() - paint.descent() + paint.ascent()) / 2

        canvas.drawText(text, x, y, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}

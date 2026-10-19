package com.jarvis.assistant.ui.island

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Compact Android rendition of the Coucou/Mochi mood character. The macOS
 * source draws the character in SwiftUI; this view keeps the same state-driven
 * idea with a canvas implementation suitable for an overlay window.
 */
class HelloAiMoodView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(74, 230, 142) }
    private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(11, 15, 22) }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 255, 255, 255) }
    private var emote = IslandEmote.CALM
    private var active = false

    fun render(emote: IslandEmote, active: Boolean, accentColor: Int) {
        this.emote = emote
        this.active = active
        fill.color = accentColor
        contentDescription = "HelloAI ${emote.assetName} ifadesi"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis() / 1000.0
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = minOf(width, height) * .34f
        val bob = if (active) (sin(now * 3.2) * radius * .08).toFloat() else 0f
        val centerX = width / 2f
        val centerY = height / 2f + bob

        canvas.drawRoundRect(RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius), radius * .64f, radius * .64f, fill)
        canvas.drawCircle(centerX - radius * .34f, centerY - radius * .14f, radius * .105f, face)
        canvas.drawCircle(centerX + radius * .34f, centerY - radius * .14f, radius * .105f, face)

        when (emote) {
            IslandEmote.THINKING -> {
                canvas.drawRect(centerX - radius * .42f, centerY - radius * .22f, centerX - radius * .18f, centerY - radius * .16f, face)
                canvas.drawCircle(centerX, centerY + radius * .30f, radius * .07f, face)
                canvas.drawCircle(centerX + radius * .24f, centerY + radius * .30f, radius * .07f, face)
            }
            IslandEmote.LISTENING -> {
                canvas.drawRoundRect(RectF(centerX - radius * .36f, centerY + radius * .15f, centerX + radius * .36f, centerY + radius * .31f), radius, radius, face)
                canvas.drawCircle(centerX + radius * .52f, centerY - radius * .42f, radius * .07f, accent)
                canvas.drawCircle(centerX + radius * .68f, centerY - radius * .56f, radius * .045f, accent)
            }
            IslandEmote.ERROR -> {
                canvas.rotate(-22f, centerX - radius * .34f, centerY - radius * .14f)
                canvas.drawRect(centerX - radius * .49f, centerY - radius * .20f, centerX - radius * .20f, centerY - radius * .12f, face)
                canvas.rotate(44f, centerX + radius * .34f, centerY - radius * .14f)
                canvas.drawRect(centerX + radius * .20f, centerY - radius * .20f, centerX + radius * .49f, centerY - radius * .12f, face)
                canvas.rotate(-22f, centerX + radius * .34f, centerY - radius * .14f)
                canvas.drawRect(centerX - radius * .28f, centerY + radius * .31f, centerX + radius * .28f, centerY + radius * .39f, face)
            }
            IslandEmote.FINISHED, IslandEmote.PROUD -> {
                accent.style = Paint.Style.STROKE
                accent.strokeWidth = radius * .11f
                accent.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(centerX - radius * .33f, centerY + radius * .20f, centerX - radius * .07f, centerY + radius * .43f, accent)
                canvas.drawLine(centerX - radius * .07f, centerY + radius * .43f, centerX + radius * .40f, centerY + radius * .02f, accent)
                accent.style = Paint.Style.FILL
            }
            IslandEmote.TIRED -> {
                canvas.drawRect(centerX - radius * .48f, centerY - radius * .20f, centerX - radius * .20f, centerY - radius * .13f, face)
                canvas.drawRect(centerX + radius * .20f, centerY - radius * .20f, centerX + radius * .48f, centerY - radius * .13f, face)
                canvas.drawOval(RectF(centerX - radius * .22f, centerY + radius * .12f, centerX + radius * .22f, centerY + radius * .32f), face)
            }
            IslandEmote.QUESTION -> {
                canvas.drawCircle(centerX, centerY + radius * .20f, radius * .10f, face)
                canvas.drawRoundRect(RectF(centerX - radius * .10f, centerY - radius * .04f, centerX + radius * .24f, centerY + radius * .08f), radius, radius, face)
            }
            else -> {
                canvas.drawArc(RectF(centerX - radius * .34f, centerY, centerX + radius * .34f, centerY + radius * .42f), 0f, 180f, false, face)
            }
        }
        if (active) postInvalidateDelayed(32L)
    }
}

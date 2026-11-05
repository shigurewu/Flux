package com.huanchengfly.tieba.post.components.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan

/**
 * 个人表情（贴吧表情包）图片 Span，仿 [EmoticonSpanV2] 基线对齐绘制。
 *
 * 占位宽度 = 行高 × clamp(真实宽高比, 0.4~3.0)，`draw()` 内按真实宽高比
 * 在占位区域内居中绘制，极端长宽比只留白不拉伸。
 *
 * @param width 表情原始宽度（解析自 sharpText，<=0 时按 1:1 处理）
 * @param height 表情原始高度（解析自 sharpText，<=0 时按 1:1 处理）
 * @param size 行高（px），即绘制区域高度
 */
class MemeImageSpan(
    drawable: Drawable,
    width: Int,
    height: Int,
    private val size: Int,
) : ImageSpan(drawable, ImageSpan.ALIGN_BASELINE) {

    private val aspectRatio: Float =
        if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f

    /** 占位宽度（用于排版/换行计算） */
    private val slotWidth: Int = (size * aspectRatio.coerceIn(0.4f, 3f)).toInt().coerceAtLeast(1)

    /** 真实宽高比下的绘制高度（不超过行高，超出部分留白） */
    private val drawHeight: Int =
        minOf(size, (slotWidth / aspectRatio).toInt()).coerceAtLeast(1)

    /** 真实宽高比下的绘制宽度 */
    private val drawWidth: Int = (drawHeight * aspectRatio).toInt().coerceAtLeast(1)

    override fun getDrawable(): Drawable {
        val drawable = super.getDrawable()
        drawable.setBounds(0, 0, drawWidth, drawHeight)
        return drawable
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            val fontFm = paint.fontMetricsInt
            fm.ascent = fontFm.top
            fm.descent = fontFm.bottom
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return slotWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val fm = paint.fontMetricsInt
        val drawable = getDrawable()
        val transY = y - drawable.bounds.bottom + fm.descent
        val transX = x + (slotWidth - drawable.bounds.width()) / 2f
        canvas.save()
        canvas.translate(transX, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }
}

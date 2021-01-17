@file:Suppress("unused")

package com.michael.ytremote.utils

import android.util.Size
import android.util.SizeF
import kotlin.math.roundToInt

/**
 * 矩形領域のリサイズ方法
 */
enum class FitMode {
    Width,       // 指定された幅になるように高さを調整
    Height,      // 指定された高さになるよう幅を調整
    Inside,      // 指定された矩形に収まるよう幅、または、高さを調整
    Fit          // 指定された矩形にリサイズする
}

/**
 * ビデオや画像のサイズ(original)を、指定されたmode:FitModeに従って、配置先のサイズ(layout)に合わせるように変換して resultに返す。
 *
 * @param original  元のサイズ（ビデオ/画像のサイズ）
 * @param layout    レイアウト先の指定サイズ
 * @param mode      計算方法の指定
 * @return          計算結果
 */
fun fitSizeTo(original:SizeF, layout:SizeF, mode:FitMode): SizeF {
    return try {
        when (mode) {
            FitMode.Fit -> layout
            FitMode.Width -> SizeF(layout.width, original.height * layout.width / original.width)
            FitMode.Height -> SizeF(original.width * layout.height / original.height, layout.height)
            FitMode.Inside -> {
                val rw = layout.width / original.width
                val rh = layout.height / original.height
                if (rw < rh) {
                    SizeF(layout.width, original.height * rw)
                } else {
                    SizeF(original.width * rh, layout.height)
                }
            }
        }
    } catch(e:Exception) {
        UtLogger.error(e.toString())
        Fitter.zeroSize
    }
}

fun SizeF.isEmpty() : Boolean {
    return width == 0f || height == 0f
}
fun SizeF.isZero() : Boolean {
    return width == 0f && height == 0f
}

fun SizeF.toSize() : Size {
    return Size(width.roundToInt(),height.roundToInt())
}
fun Size.toSizeF() : SizeF {
    return SizeF(width.toFloat(),height.toFloat())
}

interface ILayoutHint {
    val fitMode: FitMode
    val layoutWidth: Float
    val layoutHeight: Float
}

open class Fitter(override var fitMode: FitMode = FitMode.Inside, private var layoutSize: SizeF = SizeF(1000f, 1000f)) : ILayoutHint {
    override val layoutWidth: Float
        get() = layoutSize.width
    override val layoutHeight: Float
        get() = layoutSize.height


    fun setHint(fitMode:FitMode, width:Float, height:Float) {
        this.fitMode = fitMode
        layoutSize = SizeF(width,height)
    }
    fun setHint(fitMode:FitMode, layoutSize:SizeF) {
        this.fitMode = fitMode
        this.layoutSize = layoutSize
    }

    fun fit(original:SizeF):SizeF {
        return fitSizeTo(original, layoutSize, fitMode)
    }

    fun fit(w:Float, h:Float):SizeF {
        return fit(SizeF(w,h))
    }

    companion object {
        val zeroSize = SizeF(0f,0f)
    }
}
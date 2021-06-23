@file:Suppress("unused")

package io.github.toyota32k.ytremote.utils

import android.util.Size
import android.util.SizeF
import io.github.toyota32k.utils.UtLogger
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
 * @param srcWidth
 * @param srcHeight  元のサイズ（ビデオ/画像のサイズ）
 * @param dstWidth
 * @param dstHeight レイアウト先の指定サイズ
 * @param mode      計算方法の指定
 * @return          計算結果
 */
fun fitSizeTo(srcWidth:Float, srcHeight:Float, dstWidth:Float, dstHeight:Float, mode:FitMode): SizeF {
    return try {
        when (mode) {
            FitMode.Fit -> SizeF(dstWidth, dstHeight)
            FitMode.Width -> SizeF(dstWidth, srcHeight * dstWidth / srcWidth)
            FitMode.Height -> SizeF(srcWidth * dstHeight / srcHeight, dstHeight)
            FitMode.Inside -> {
                val rw = dstWidth / srcWidth
                val rh = dstHeight / srcHeight
                if (rw < rh) {
                    SizeF(dstWidth, srcHeight * rw)
                } else {
                    SizeF(srcWidth * rh, dstHeight)
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

class Fitter {
    private var fitMode: FitMode = FitMode.Inside
    private var layoutWidth: Int = 1000
    private var layoutHeight: Int = 1000

    fun setHint(fitMode:FitMode, width:Int, height:Int):Boolean {
        if(this.fitMode==fitMode && width == layoutWidth && height==layoutHeight ) return false
        this.fitMode = fitMode
        this.layoutWidth = width
        this.layoutHeight = height
        return true
    }

    fun fit(original:Size):Size {
        return fit(original.width, original.height)
    }

    fun fit(w:Int, h:Int):Size {
        return fitSizeTo(w.toFloat(), h.toFloat(), layoutWidth.toFloat(), layoutHeight.toFloat(), fitMode).toSize()
    }

    companion object {
        val zeroSize = SizeF(0f,0f)
    }
}
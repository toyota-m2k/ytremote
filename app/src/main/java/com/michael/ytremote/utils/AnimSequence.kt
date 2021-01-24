@file:Suppress("unused")

package com.michael.ytremote.utils

import android.animation.ValueAnimator
import android.view.View

interface IAnimChip {
    val precess:(Float)->Unit
    val before:((reverse:Boolean)->Unit)?
    val after:((reverse:Boolean)->Unit)?
}

abstract class AnimChipBase (
    val start:Float,
    val end:Float,
    override val precess: (Float) -> Unit) : IAnimChip {

    override var before: ((reverse: Boolean) -> Unit)? = null
    override var after: ((reverse: Boolean) -> Unit)? = null
}

class ViewSizeAnimChip<O>(target:O, startDp:Int, endDp:Int, height:Boolean)
    : AnimChipBase(startDp.toFloat(), endDp.toFloat(), {
    if (height) {
        target.setLayoutHeight(target.context.dp2px(it).toInt())
    } else {
        target.setLayoutWidth(target.context.dp2px(it).toInt())
    }
}) where O : View



class ViewVisibilityAnimationChip<O>(target:O, startVisible:Boolean, private val endVisible:Boolean, gone:Boolean=false, maxAlpha:Float=1f)
    : AnimChipBase(bool2float(startVisible)*maxAlpha,bool2float(endVisible)*maxAlpha, {
    target.alpha = it
}) where O : View {
    init {
        before = { reverse->
            val s = if(!reverse) startVisible else endVisible
            if(!s) {
                target.alpha = 0f
                target.visibility = View.VISIBLE
            }
        }

        after = { reverse->
            val e = if(!reverse) endVisible else startVisible
            if(!e) {
                target.visibility = if(gone) View.GONE else View.INVISIBLE
            }
        }
    }

    companion object {
        fun bool2float(b: Boolean): Float {
            return if (b) 1f else 0f
        }

        fun float2bool(f: Float): Boolean {
            return if (f == 0f) false else true
        }
    }
}

abstract class AnimDriver(du:Long) {
    val animator:ValueAnimator = ValueAnimator.ofFloat(0f,1f).apply {
        duration = du
        addUpdateListener {

        }
    }
    val list:MutableList<IAnimChip> = mutableListOf()

    fun animate(reverse:Boolean) {
        for(v in list) {
            v.before?.invoke(reverse)
        }

        for(v in list) {
            v.before?.invoke(reverse)
        }
    }


}

class AnimSet : IAnimChip {

}

//class AnimSequence : IAnim {
//
//}
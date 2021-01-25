@file:Suppress("unused")

package com.michael.ytremote.utils

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface IAnimChip {
    fun process(r:Float)
    fun before(reverse:Boolean)
    fun after(reverse:Boolean)
}

interface IAnimEngine {
    fun animate(reverse: Boolean, completed:((Boolean)->Unit)?)
    fun cancel()
}

abstract class AbstractAnimChip : IAnimChip {
    override fun before(reverse: Boolean) {
    }

    override fun after(reverse: Boolean) {
    }
}

class ViewSizeAnimChip<O>(private val target:O, private val startDp:Int, private val endDp:Int, val height:Boolean)
    : AbstractAnimChip() where O : View {
    override fun process(r: Float) {
        if (height) {
            target.setLayoutHeight(target.context.dp2px(startDp + r * (endDp - startDp)).toInt())
        } else {
            target.setLayoutWidth(target.context.dp2px(startDp + r * (endDp - startDp)).toInt())
        }
    }
}



class ViewVisibilityAnimationChip<O>(private val target:O, private val startVisible:Boolean, private val endVisible:Boolean, private val gone:Boolean=false, private val maxAlpha:Float=1f)
    : AbstractAnimChip() where O : View {
    override fun process(r: Float) {
        target.alpha = r * maxAlpha
    }

    override fun before(reverse: Boolean) {
        val s = if(!reverse) startVisible else endVisible
        if(!s) {
            target.alpha = 0f
            target.visibility = View.VISIBLE
        }
    }

    override fun after(reverse: Boolean) {
        val e = if(!reverse) endVisible else startVisible
        if(!e) {
            target.visibility = if(gone) View.GONE else View.INVISIBLE
        }
    }
}

class AnimSet(private val duration:Long) : AbstractAnimChip(), IAnimEngine {
    private val list:MutableList<IAnimChip> = mutableListOf()
    private var activeAnimator: ValueAnimator? = null
    private var next:Boolean? = null

    override fun cancel() {
        activeAnimator?.run {
            cancel()
        }
    }

    override fun animate(reverse:Boolean, completed: ((Boolean) -> Unit)?) {
        activeAnimator?.run {
            cancel()
            next = reverse
            return
        }

        before(reverse)

        activeAnimator = ValueAnimator.ofFloat(0f,1f).apply {
            duration = this@AnimSet.duration
            addUpdateListener {
                process(it.animatedValue as Float)
            }
            doOnEnd {
                after(reverse)
                activeAnimator = null
                tryContinue(completed)
            }
            doOnCancel {
                activeAnimator = null
                completed?.invoke(false)
            }
            start()
        }
    }

    override fun process(r: Float) {
        for(v in list) {
            v.process(r)
        }
    }

    override fun before(reverse: Boolean) {
        for(v in list) {
            v.before(reverse)
        }
    }

    override fun after(reverse: Boolean) {
        for(v in list) {
            v.after(reverse)
        }
    }
    
    private fun tryContinue(completed: ((Boolean) -> Unit)?) {
        next?.let {
            next = null
            animate(it, completed)
        } ?: completed?.invoke(true)
    }

    fun add(clip:IAnimChip) : AnimSet {
        list.add(clip)
        return this
    }
}

class AnimSequence : IAnimEngine {
    private val list = mutableListOf<IAnimEngine>()
    private var currentEngine: IAnimEngine? = null
    private var next:Boolean? = null

    fun add(engine:IAnimEngine):AnimSequence {
        list.add(engine)
        return this
    }


    override fun cancel() {
        currentEngine?.run {
            cancel()
        }
    }

    private suspend fun animateSub(engine:IAnimEngine, reverse:Boolean) = suspendCoroutine<Boolean> { cont->
        currentEngine = engine
        engine.animate(reverse) {
            cont.resume(it)
        }
    }

    override fun animate(reverse: Boolean, completed: ((Boolean) -> Unit)?) {
        currentEngine?.run {
            cancel()
            next = reverse
            return
        }

        val list: Iterable<IAnimEngine> = if (reverse) list.reverse() else list
        MainScope().launch {
            for (v in list) {
                if(!animateSub(v, reverse)) {
                    currentEngine = null
                    return@launch
                }
            }
            tryContinue(completed)
        }
    }

    private fun tryContinue(completed: ((Boolean) -> Unit)?) {
        next?.let {
            next = null
            animate(it, completed)
        } ?: completed?.invoke(true)
    }

}

//class AnimSequence : IAnim {
//
//}
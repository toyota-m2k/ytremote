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
    fun animate(reverse: Boolean, completed:((Boolean)->Unit)?=null)
    fun cancel()
}

abstract class AbstractAnimChip : IAnimChip {
    override fun before(reverse: Boolean) {
//        UtLogger.debug(toString()+" before(${reverse})")
    }

    override fun after(reverse: Boolean) {
//        UtLogger.debug(toString()+" after(${reverse})")
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

    override fun toString(): String {
        return super.toString() + "${target}"
    }
}



class ViewVisibilityAnimationChip<O>(private val target:O, private val startVisible:Boolean, private val endVisible:Boolean, private val gone:Boolean=false, private val maxAlpha:Float=1f)
    : AbstractAnimChip() where O : View {
    override fun process(r: Float) {
        val alpha = if(startVisible) 1-r else r
        target.alpha = alpha * maxAlpha
    }

    override fun before(reverse: Boolean) {
//        super.before(reverse)
        val e = if(reverse) startVisible else endVisible
        if(e) {
            target.alpha = 0f
            target.visibility = View.VISIBLE
        }
    }

    override fun after(reverse: Boolean) {
//        super.after(reverse)
        val e = if(reverse) startVisible else endVisible
        if(!e) {
            target.visibility = if(gone) View.GONE else View.INVISIBLE
        }
    }

    override fun toString(): String {
        return super.toString() + "${target}"
    }
}

class AnimSet(private val duration:Long=300L) : AbstractAnimChip(), IAnimEngine {
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
//            UtLogger.debug("AnimSet.animate cancelled")
            cancel()
            next = reverse
            return
        }

        before(reverse)

        activeAnimator = (if(reverse) ValueAnimator.ofFloat(1f,0f) else ValueAnimator.ofFloat(0f,1f)).apply {
            duration = this@AnimSet.duration
            addUpdateListener {
                process(it.animatedValue as Float)
            }
            doOnEnd {
                after(reverse)
                tryContinue(true, completed)
            }
            doOnCancel {
                tryContinue(false, completed)
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
//        UtLogger.debug("AnimSet.animate before")
        for(v in list) {
            v.before(reverse)
        }
    }

    override fun after(reverse: Boolean) {
//        UtLogger.debug("AnimSet.animate after")
        for(v in list) {
            v.after(reverse)
        }
    }
    
    private fun tryContinue(result:Boolean, completed: ((Boolean) -> Unit)?) {
//        UtLogger.debug("AnimSet.animate tryContinue")
        if(activeAnimator!=null) {
            activeAnimator = null
            next?.let {
                next = null
                animate(it, completed)
            } ?: completed?.invoke(result)
        }
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
//        UtLogger.debug("AnimSequence begin: animateSub(${reverse}): ${engine}")
        currentEngine = engine
        engine.animate(reverse) {
//            UtLogger.debug("AnimSequence completed: animateSub(${reverse}): ${engine}")
            cont.resume(it)
        }
    }

    override fun animate(reverse: Boolean, completed: ((Boolean) -> Unit)?) {
        currentEngine?.run {
//            UtLogger.debug("AnimSequence: animate(${reverse}): cancelling")
            cancel()
            next = reverse
            return
        }
//        UtLogger.debug("AnimSequence begin: animate(${reverse})")

        val list: Iterable<IAnimEngine> = if (reverse) list.reverse() else list
        MainScope().launch {
            var result = true
            for (v in list) {
                if(!animateSub(v, reverse)) {
//                    UtLogger.debug("AnimSequence: animate(${reverse}): cancelled")
                    result = false
                    break
                }
            }
            tryContinue(result, completed)
        }
    }

    private fun tryContinue(result:Boolean, completed: ((Boolean) -> Unit)?) {
//        UtLogger.debug("AnimSequence: tryContinue")
        if(currentEngine != null) {
            currentEngine = null
            next?.let {
                next = null
                animate(it, completed)
            } ?: completed?.invoke(result)
        }
    }
}

//class AnimSequence : IAnim {
//
//}
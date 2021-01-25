package com.michael.ytremote.utils

import android.animation.ValueAnimator
import android.view.View
import androidx.core.animation.addListener

//@Suppress("unused")
//open class AnimPack(private val duration:Long=300) {
//    data class AnimItem(val target:Any, val a:Float, val b:Float, val fn:((Any,Float)->Unit)) {
//        fun valueAt(f:Float, reverse:Boolean) : Float {
//            val r = if(reverse) 1-f else f
//            return a + r*(b-a)
//        }
//    }
//
//    protected val list = mutableListOf<AnimItem>()
//
//    var onStart:((reverse:Boolean)->Unit)? = null
//    var onCompleted:((reverse:Boolean)->Unit)? = null
//
//    fun animate(reverse:Boolean) {
//        ValueAnimator.ofFloat(0f,1f)?.apply {
//            addUpdateListener {
//                for (v in list) {
//                    v.fn(v.target, v.valueAt(it.animatedValue as Float, reverse))
//                }
//            }
//            addListener(onEnd = {
//                onCompleted?.invoke(reverse)
//            })
//            duration = this@AnimPack.duration
//            onStart?.invoke(reverse)
//            start()
//        }
//    }
//
//    fun go() {
//        animate(false)
//    }
//
//    fun reverse() {
//        animate(true)
//    }
//
//
//    operator fun plusAssign(anim:AnimItem) {
//        list.add(anim)
//    }
//
//    fun addAnim(target:Any, a:Float, b:Float, fn:((Any,Float)->Unit)) {
//        this+=AnimItem(target, a, b, fn)
//    }
//}
//
//class VisibilityAnimPack(duration:Long=300) : AnimPack(duration) {
//    init {
//        onStart = { reverse->
//            for(v in list) {
//                val x = if(reverse) v.b else v.a
//                if(x==0f && v.target is View) {
//                    v.target.alpha = 0f
//                    v.target.visibility = View.VISIBLE
//                }
//            }
//        }
//        onCompleted = { reverse->
//            for(v in list) {
//                val x = if(reverse) v.a else v.b
//                if(x==0f && v.target is View) {
//                    v.target.visibility = View.INVISIBLE
//                }
//            }
//        }
//    }
//
//    fun addView(view: View, hideNormally:Boolean=false) {
////        fun boolToFloat(b:Boolean):Float  {
////            return if(b) 0f else 1f
////        }
//        val a = if(hideNormally) 0f else 1f
//        val b = if(hideNormally) 1f else 0f
//        addAnim(view, a, b) {o,v->
//            (o as View).alpha = v
//        }
//    }
//}
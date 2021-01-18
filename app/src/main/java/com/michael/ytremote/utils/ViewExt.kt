@file:Suppress("unused")

package com.michael.ytremote.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Parcel
import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified T> Context.findSpecialContext() : T? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is T) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

fun Context.activity(): Activity? {
    return findSpecialContext()
}
fun Context.fragment(): Fragment? {
    return findSpecialContext()
}

fun Context.lifecycleOwner() : LifecycleOwner? {
    return findSpecialContext()
}

fun Context.viewModelStorageOwner(): ViewModelStoreOwner? {
    return findSpecialContext()
}

fun View.activity(): FragmentActivity? {
    return context?.findSpecialContext()
}
fun View.fragment(): Fragment? {
    return context?.findSpecialContext()
}
fun View.viewModelStorageOwner(): ViewModelStoreOwner? {
    return context?.findSpecialContext()
}
fun View.lifecycleOwner(): LifecycleOwner? {
    return context?.findSpecialContext()
}



fun View.setLayoutWidth(width:Int) {
    val params = layoutParams
    if(null!=params) {
        params.width = width
        layoutParams = params
    }
}

fun View.getLayoutWidth() : Int {
    return if(layoutParams?.width ?: -1 >=0) {
        layoutParams.width
    } else {
        width
    }
}

fun View.setLayoutHeight(height:Int) {
    val params = layoutParams
    if(null!=params) {
        params.height = height
        layoutParams = params
    }
}

@Suppress("unused")
fun View.getLayoutHeight() : Int {
    return if(layoutParams?.height ?: -1 >=0) {
        layoutParams.height
    } else {
        height
    }
}

fun View.setLayoutSize(width:Int, height:Int) {
    val params = layoutParams
    if(null!=params) {
        params.width = width
        params.height = height
        layoutParams = params
    }
}

@Suppress("unused")
fun View.measureAndGetSize() : Size {
    this.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    return Size(this.measuredWidth, this.measuredHeight)
}

fun View.setMargin(left:Int, top:Int, right:Int, bottom:Int) {
    val p = layoutParams as? ViewGroup.MarginLayoutParams
    if(null!=p) {
        p.setMargins(left, top, right, bottom)
        layoutParams = p
    }

}

fun Parcel.writeBool(v:Boolean) {
    writeInt(if(v) 1 else 0)
}
fun Parcel.readBool() : Boolean {
    return readInt() != 0
}
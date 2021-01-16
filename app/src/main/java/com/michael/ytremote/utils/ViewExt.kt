package com.michael.ytremote.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.Fragment
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

fun View.activity(): Activity? {
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

@file:Suppress("unused")

package com.michael.bindit

import android.view.View
import io.reactivex.rxjava3.disposables.Disposable

@Suppress("MemberVisibilityCanBePrivate")
open class Binder : Disposable {
    protected var disposed: Boolean = false
    protected val bindings = mutableListOf<Disposable>()
    override fun dispose() {
        if(!disposed) {
            disposed = true
            reset()
        }
    }

    override fun isDisposed(): Boolean {
        return disposed
    }

    fun register(vararg bindings:IBinding):Binder {
        for(b in bindings) {
            this.bindings.add(b)
        }
        return this
    }

    fun reset() {
        bindings.forEach { it.dispose() }
        bindings.clear()
    }
}

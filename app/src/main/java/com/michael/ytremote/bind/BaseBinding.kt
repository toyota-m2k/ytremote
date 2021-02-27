package com.michael.bindit.impl

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.michael.bindit.BindingMode
import com.michael.bindit.IBinding


abstract class DisposableImpl : IBinding {
    protected abstract fun cleanup()

    private var alive:Boolean = true

    override fun dispose() {
        if(alive) {
            alive = false
            cleanup()
        }
    }

    override fun isDisposed(): Boolean {
        return !alive
    }
}

abstract class BaseBinding<T>(override val mode: BindingMode) : DisposableImpl() {
    abstract val data: LiveData<T>
    open var view: View? = null
    private var dataObserver:Observer<T?>? = null

    fun connect(owner:LifecycleOwner, view:View) {
        this.view = view
        if(mode!=BindingMode.OneWayToSource) {
            dataObserver = Observer<T?> {
                onDataChanged(it)
            }
            data.observe(owner,dataObserver!!)
            // data.value==null のときobserveのタイミングでonDataChanged()が呼ばれないような現象があったので明示的に呼び出しておく。
            if(data.value==null) {
                onDataChanged(data.value)
            }
        }
    }

    protected abstract fun onDataChanged(v:T?)

    override fun cleanup() {
        view = null
        val ob = dataObserver ?: return
        data.removeObserver(ob)
    }
}
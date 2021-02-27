package com.michael.bindit.util

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.reactivex.rxjava3.disposables.Disposable

interface ListenerKey:Disposable

@Suppress("unused")
class Listeners<T> {
    interface IListener<T> {
        fun onChanged(value:T)
    }

    private val functions = mutableListOf<(OwnerWrapper)>()

    inner class OwnerWrapper(owner:LifecycleOwner, val fn:(T)->Unit) : LifecycleEventObserver, ListenerKey {
        var lifecycle:Lifecycle?
        init {
            lifecycle = owner.lifecycle.also {
                it.addObserver(this)
            }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if(!source.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                dispose()
            }
        }

        @MainThread
        override fun dispose() {
            lifecycle?.let {
                lifecycle = null
                it.removeObserver(this)
                functions.remove(this)
            }
        }

        private val alive:Boolean
            get() = lifecycle !=null

        override fun isDisposed(): Boolean {
            return lifecycle == null
        }

        @MainThread
        fun invoke(arg:T) {
            if(alive) {
                fn(arg)
            }
        }
    }


    @MainThread
    fun add(owner:LifecycleOwner, fn:(T)->Unit): ListenerKey {
        return OwnerWrapper(owner, fn).apply {
            functions.add(this)
        }
    }

    @MainThread
    fun add(owner: LifecycleOwner, listener:IListener<T>):ListenerKey {
        return add(owner, listener::onChanged)
    }

    @MainThread
    fun remove(key:ListenerKey) {
        key.dispose()
    }

    @MainThread
    fun clear() {
        while(functions.isNotEmpty()) {
            functions.last().dispose()
        }
    }

    @MainThread
    fun invoke(v:T) {
        functions.forEach {
            it.invoke(v)
        }
    }
}



package io.github.toyota32k.ytremote.model

import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.Listeners
import java.util.concurrent.atomic.AtomicInteger

/**
 * 参照カウンタクラス
 * 参照カウンタがゼロに戻ったとき、リスナーに通知する。
 * AppViewModel の参照カウンタとして使用する。
 */
class RefCount : IDisposable{
    private val refCount = AtomicInteger(0)
    val listeners = Listeners<Boolean>()
    val value:Int get() = refCount.get()

    fun observeRelease(released:()->Unit) {
        listeners.addForever { released() }
    }

    fun addRef() {
        refCount.incrementAndGet()
    }

    fun release() {
        if(refCount.decrementAndGet()<=0) {
            listeners.invoke(false)
        }
    }

    override fun dispose() {
        listeners.clear()
    }

    override fun isDisposed(): Boolean {
        return false
    }

    operator fun inc() : RefCount {
        addRef()
        return this
    }
    operator fun dec() : RefCount {
        release()
        return this
    }
}
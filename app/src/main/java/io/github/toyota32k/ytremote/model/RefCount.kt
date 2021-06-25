package io.github.toyota32k.ytremote.model

import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.Listeners
import io.github.toyota32k.ytremote.BooApplication
import java.util.concurrent.atomic.AtomicInteger

/**
 * 参照カウンタクラス
 * 参照カウンタがゼロに戻ったとき、リスナーに通知する。
 * AppViewModel の参照カウンタとして使用する。
 */
class RefCount : IDisposable{
    private val refCount = AtomicInteger(0)
    private val listeners = Listeners<Boolean>()
    val value:Int get() = refCount.get()

    fun observeRelease(released:()->Unit) {
        listeners.addForever {
            BooApplication.logger.debug("released.")
            released()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun addRef() {
        BooApplication.logger.debug("$refCount")
        refCount.incrementAndGet()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun release() {
        BooApplication.logger.debug("$refCount")
        if(refCount.decrementAndGet()<=0) {
            BooApplication.logger.debug("will be released.")
            listeners.invoke(false)
        }
    }

    override fun dispose() {
        BooApplication.logger.debug("disposed.")
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
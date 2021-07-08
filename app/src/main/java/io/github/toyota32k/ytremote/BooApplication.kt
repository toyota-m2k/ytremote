package io.github.toyota32k.ytremote

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.ytremote.model.AppViewModel

class BooApplication : Application(), ViewModelStoreOwner {
    private var viewModelStore : ViewModelStore? = null

    override fun getViewModelStore(): ViewModelStore {
        if(viewModelStore==null) {
            viewModelStore = ViewModelStore()
        }
        return viewModelStore!!
    }

    fun releaseViewModelStore() {
        viewModelStore?.clear()
        viewModelStore = null
    }

    init {
        instance_ = this
    }

    override fun onTerminate() {
        logger.debug()
        super.onTerminate()
        releaseViewModelStore()
    }

    companion object {
        private lateinit var instance_:BooApplication
        val instance get() = instance_
        val logger = UtLog("App", omissionNamespace = "io.github.toyota32k.ytremote")
    }
}

package io.github.toyota32k.ytremote

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.github.toyota32k.ytremote.model.AppViewModel

class BooApplication : Application(), ViewModelStoreOwner {
    private var viewModelStore : ViewModelStore? = null

    override fun getViewModelStore(): ViewModelStore {
        var vms = viewModelStore
        if(null==vms) {
            vms = ViewModelStore()
            viewModelStore = vms
        }
        return vms
    }

    init {
        instance_ = this
    }

    override fun onCreate() {
        super.onCreate()
        AppViewModel.instance.refCount.observeRelease {
            viewModelStore?.clear()
            viewModelStore = null
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        viewModelStore?.clear()
        viewModelStore = null
    }

    companion object {
        private lateinit var instance_:BooApplication
        val instance get() = instance_
    }
}

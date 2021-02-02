package com.michael.ytremote

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.michael.ytremote.model.AppViewModel

class BooApplication : Application(), ViewModelStoreOwner {
    private val viewModelStore = ViewModelStore()

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    init {
        instance_ = this
    }

    override fun onCreate() {
        super.onCreate()
        AppViewModel.instance.refCount.observeForever { refCount->
            if(null!=refCount) {
                if(refCount==0) {
                    viewModelStore.clear()
                }
            }
        }
    }

    companion object {
        private lateinit var instance_:BooApplication
        val instance
            get() = instance_
    }
}

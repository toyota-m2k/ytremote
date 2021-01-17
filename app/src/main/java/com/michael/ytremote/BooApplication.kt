package com.michael.ytremote

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class BooApplication : Application(), ViewModelStoreOwner {

    override fun getViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }

    companion object {
        fun instance(activity:Activity) :BooApplication {
            return activity.application as BooApplication
        }
    }
}

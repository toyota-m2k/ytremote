@file:Suppress("unused")

package com.michael.bindit.impl

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.michael.bindit.BindingMode
import com.michael.bindit.BoolConvert

open class EnableBinding(
        rawData: LiveData<Boolean>,
        boolConvert: BoolConvert = BoolConvert.Staright
) : BaseBinding<Boolean>(BindingMode.OneWay) {

    override val data: LiveData<Boolean> = if(boolConvert==BoolConvert.Inverse) rawData.map { !it } else rawData

    override fun onDataChanged(v: Boolean?) {
        val view = this.view ?: return
        val enabled = v==true
        view.isEnabled = enabled
        view.focusable = if(enabled) View.FOCUSABLE_AUTO else View.NOT_FOCUSABLE
        view.isClickable = enabled
        view.isFocusableInTouchMode = enabled
    }
    companion object {
        fun create(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Staright) : EnableBinding {
            return EnableBinding(data, boolConvert).apply { connect(owner, view) }
        }
    }
}
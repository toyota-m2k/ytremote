@file:Suppress("unused")

package com.michael.bindit.impl

import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.michael.bindit.BindingMode
import com.michael.bindit.BoolConvert

open class CheckBinding protected constructor(
    rawData: LiveData<Boolean>,
    boolConvert:BoolConvert,
    mode: BindingMode
) : BaseBinding<Boolean>(mode) {
    constructor(data:LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Staright) : this(data,boolConvert,BindingMode.OneWay)

    override val data: LiveData<Boolean> = if(boolConvert==BoolConvert.Inverse) rawData.map { !it } else rawData

    val compoundButton:CompoundButton?
        get() = view as? CompoundButton

    open fun connect(owner: LifecycleOwner, view:CompoundButton) {
        super.connect(owner,view)
    }

    override fun onDataChanged(v: Boolean?) {
        val view = compoundButton ?: return
        val chk = (v==true)
        if(view.isChecked != chk) {
            view.isChecked = chk
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: CompoundButton, data: LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Staright):CheckBinding {
            return CheckBinding(data, boolConvert, BindingMode.OneWay).apply { connect(owner, view) }
        }
        fun create(owner: LifecycleOwner, view:CompoundButton, data: MutableLiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Staright, mode: BindingMode=BindingMode.TwoWay):CheckBinding {
            return if(mode==BindingMode.OneWay) {
                CheckBinding(data, boolConvert, BindingMode.OneWay).apply { connect(owner, view) }
            } else {
                MutableCheckBinding.create(owner, view, data, boolConvert, mode)
            }
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class MutableCheckBinding(
        protected val mutableData: MutableLiveData<Boolean>,
        protected val boolConvert: BoolConvert = BoolConvert.Staright,
        mode: BindingMode = BindingMode.TwoWay
): CheckBinding(mutableData,boolConvert,mode), CompoundButton.OnCheckedChangeListener {

    override fun connect(owner: LifecycleOwner, view: CompoundButton) {
        super.connect(owner, view)
        if(mode!=BindingMode.OneWay) {
            view.setOnCheckedChangeListener(this)
            onCheckedChanged(view, view.isChecked)
        }
    }

    override fun cleanup() {
        if(mode!=BindingMode.OneWay) {
            compoundButton?.setOnCheckedChangeListener(null)
        }
        super.cleanup()
    }

    // region OnCheckedChangeListener

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val chk = if(boolConvert==BoolConvert.Inverse) !isChecked else isChecked
        if(mutableData.value!=chk) {
            mutableData.value = chk
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: CompoundButton, data: MutableLiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Staright, mode: BindingMode=BindingMode.TwoWay):MutableCheckBinding {
            return MutableCheckBinding(data, boolConvert, mode).apply { connect(owner, view) }
        }
    }
    // endregion
}
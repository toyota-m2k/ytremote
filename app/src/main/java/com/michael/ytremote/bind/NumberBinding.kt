package com.michael.bindit.impl

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.michael.bindit.BindingMode

open class NumberBinding<N> protected constructor(
    data: LiveData<N>,
    mode: BindingMode
) : TextBinding(data.map { it.toString() }, mode) where N : Number {
    constructor(data: LiveData<N>):this(data, BindingMode.OneWay)
    // region Observer (LiveData-->View)

    override fun onDataChanged(v: String?) {
        val view = textView?:return
        if(v!=view.text) {
            view.text = v
        }
    }

    // endregion

    companion object {
        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Int>): IntBinding {
            return IntBinding(data).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Long>): LongBinding {
            return LongBinding(data).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Float>): FloatBinding {
            return FloatBinding(data).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: TextView, data: LiveData<Double>): DoubleBinding {
            return DoubleBinding(data).apply { connect(owner, view) }
        }


        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Int>, mode: BindingMode = BindingMode.TwoWay): MutableIntBinding {
            return MutableIntBinding(data, mode).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Long>, mode: BindingMode = BindingMode.TwoWay): MutableLongBinding {
            return MutableLongBinding(data, mode).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Float>, mode: BindingMode = BindingMode.TwoWay): MutableFloatBinding {
            return MutableFloatBinding(data, mode).apply { connect(owner, view) }
        }

        fun create(owner: LifecycleOwner, view: EditText, data: MutableLiveData<Double>, mode: BindingMode = BindingMode.TwoWay): MutableDoubleBinding {
            return MutableDoubleBinding(data, mode).apply { connect(owner, view) }
        }
    }
}

open class MutableNumberBinding<N>(
    val mutableData: MutableLiveData<N>,
    private val toNumber:(String)->N,
    mode: BindingMode = BindingMode.TwoWay
) : NumberBinding<N>(mutableData, mode), TextWatcher where N : Number {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        val nx = toNumber(s?.toString() ?: "")
        if(nx!=mutableData.value) {
            mutableData.value = nx
        }
    }

    private val editText: EditText?
        get() = view as EditText?

    fun connect(owner: LifecycleOwner, view: EditText) {
        super.connect(owner,view)
        if(mode!= BindingMode.OneWay) {
            view.addTextChangedListener(this)
            afterTextChanged(view.text)
        }
    }

    override fun cleanup() {
        editText?.removeTextChangedListener(this)
        super.cleanup()
    }
}

class IntBinding(data: LiveData<Int>) : NumberBinding<Int>(data)
class MutableIntBinding(data: MutableLiveData<Int>,mode:BindingMode=BindingMode.TwoWay) : MutableNumberBinding<Int>(data, {it.toInt()}, mode)

class LongBinding(data: LiveData<Long>) : NumberBinding<Long>(data)
class MutableLongBinding(data: MutableLiveData<Long>,mode:BindingMode=BindingMode.TwoWay) : MutableNumberBinding<Long>(data, {it.toLong()}, mode)

class DoubleBinding(data: LiveData<Double>) : NumberBinding<Double>(data)
class MutableDoubleBinding(data: MutableLiveData<Double>,mode:BindingMode=BindingMode.TwoWay) : MutableNumberBinding<Double>(data, {it.toDouble()}, mode)

class FloatBinding(data: LiveData<Float>) : NumberBinding<Float>(data)
class MutableFloatBinding(data: MutableLiveData<Float>,mode:BindingMode=BindingMode.TwoWay) : MutableNumberBinding<Float>(data, { try{it.toFloat()} catch(_:Throwable){0f} }, mode)

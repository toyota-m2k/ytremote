@file:Suppress("unused")

package com.michael.bindit.impl

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.michael.bindit.BindingMode

open class TextBinding protected constructor(
    override val data:LiveData<String>,
    mode:BindingMode
) : BaseBinding<String>(mode) {
    constructor(data:LiveData<String>):this(data,BindingMode.OneWay)
    // region Observer (LiveData-->View)

    protected val textView:TextView?
        get() = view as TextView?

    fun connect(owner: LifecycleOwner, view: TextView) {
        super.connect(owner,view)
    }

    override fun onDataChanged(v: String?) {
        val view = textView?:return
        if(v!=view.text) {
            view.text = v
        }
    }

    // endregion

    companion object {
        fun create(owner:LifecycleOwner, view:TextView, data:LiveData<String>) : TextBinding {
            return TextBinding(data, BindingMode.OneWay).apply { connect(owner,view) }
        }
        fun create(owner:LifecycleOwner, view:EditText, data:MutableLiveData<String>, mode:BindingMode):TextBinding {
            if(mode==BindingMode.OneWay) {
                return TextBinding(data, BindingMode.OneWay).apply { connect(owner,view) }
            }
            return MutableTextBinding.create(owner,view,data,mode)
        }
    }
}

open class MutableTextBinding(
    override val data: MutableLiveData<String>,
    mode: BindingMode = BindingMode.TwoWay
) : TextBinding(data,mode), TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        val tx = s?.toString()
        if(tx!=data.value) {
            data.value = tx
        }
    }

    private val editText:EditText?
        get() = view as EditText?

    fun connect(owner: LifecycleOwner, view:EditText) {
        super.connect(owner,view)
        if(mode!=BindingMode.OneWay) {
            view.addTextChangedListener(this)
            afterTextChanged(view.text)
        }
    }

    override fun cleanup() {
        editText?.removeTextChangedListener(this)
        super.cleanup()
    }

    // endregion
    companion object {
        fun create(owner:LifecycleOwner, view:EditText, data:MutableLiveData<String>, mode:BindingMode=BindingMode.TwoWay):MutableTextBinding {
            return MutableTextBinding(data,mode).apply { connect(owner,view) }
        }
    }
}
package com.michael.bindit.impl

import android.widget.RadioGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.michael.bindit.BindingMode

interface IIDValueResolver<T> {
    fun id2value(id:Int) : T?
    fun value2id(v:T): Int
}

open class RadioGroupBinding<T> protected constructor(
    override val data: LiveData<T>,
    mode:BindingMode
) : BaseBinding<T>(mode) {
    constructor(data:LiveData<T>):this(data,BindingMode.OneWay)

    lateinit var idResolver: IIDValueResolver<T>
    val radioGroup:RadioGroup?
        get() = view as? RadioGroup

    open fun connect(owner: LifecycleOwner, view:RadioGroup, idResolver:IIDValueResolver<T>) {
        this.idResolver = idResolver
        super.connect(owner,view)
    }

    override fun onDataChanged(v: T?) {
        val view = radioGroup ?: return
        if(v!=null) {
            val id = idResolver.value2id(v)
            if(view.checkedRadioButtonId!=id) {
                view.check(id)
            }
        }
    }
    companion object {
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: LiveData<T>, idResolver: IIDValueResolver<T>):RadioGroupBinding<T> {
            return RadioGroupBinding(data).apply { connect(owner, view, idResolver) }
        }
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode:BindingMode=BindingMode.TwoWay):RadioGroupBinding<T> {
            return MutableRadioGroupBinding(data, mode).apply { connect(owner, view, idResolver) }
        }
    }
}

open class MutableRadioGroupBinding<T>(
    override val data:MutableLiveData<T>,
    mode:BindingMode = BindingMode.TwoWay
) : RadioGroupBinding<T>(data,mode), RadioGroup.OnCheckedChangeListener {

    override fun connect(owner: LifecycleOwner, view:RadioGroup, idResolver:IIDValueResolver<T>) {
        super.connect(owner,view,idResolver)
        if(mode!=BindingMode.OneWay) {
            view.setOnCheckedChangeListener(this)
        }
    }

    override fun cleanup() {
        radioGroup?.setOnCheckedChangeListener(null)
        super.cleanup()
    }

    override fun onCheckedChanged(@Suppress("UNUSED_PARAMETER") group: RadioGroup?, checkedId: Int) {
        val v = idResolver.id2value(checkedId)
        if(data.value!=v) {
            data.value = v
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view: RadioGroup, data: MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode:BindingMode=BindingMode.TwoWay):MutableRadioGroupBinding<T> {
            return MutableRadioGroupBinding(data, mode).apply { connect(owner, view, idResolver) }
        }
    }
}
package com.michael.ytremote.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup
import java.lang.ref.WeakReference

abstract class MaterialButtonGroup {
    private var wrToggleGroup: WeakReference<MaterialButtonToggleGroup>? = null
    var owner: MaterialButtonToggleGroup?
        get() = wrToggleGroup?.get()
        protected set(v) {
            if(v!=null) {
                wrToggleGroup = WeakReference(v)
                v.addOnButtonCheckedListener { group, checkedId, isChecked ->
                    onChecked(checkedId, isChecked)
                }
            }
        }

    abstract fun onChecked(id:Int, checked:Boolean)

}

abstract class RadioButtonGroup<T>() : MaterialButtonGroup() {
    private val internalSelectedValue = MutableLiveData<T>()
    val selectedValue: LiveData<T> = internalSelectedValue
    var selected:T?
        get() = internalSelectedValue.value
        set(v) {
            if(v!=null) {
                owner?.check(value2id(v))
            } else if(internalSelectedValue.value!=null) {
                owner?.uncheck(value2id(internalSelectedValue.value!!))
            }
            internalSelectedValue.value = v
        }

    fun bind(gr: MaterialButtonToggleGroup, sourceToView:Boolean=true) {
        utAssert(gr.isSingleSelection)

        owner = gr
        if(sourceToView) {
            val sel = selected
            gr.clearChecked()
            if(sel!=null) {
                gr.check(value2id(sel))
            }
        } else {
            internalSelectedValue.value = id2value(gr.checkedButtonId)
        }
    }

    override fun onChecked(id: Int, checked: Boolean) {
        if(checked) {
            internalSelectedValue.value = id2value(id)
        }
    }

    abstract fun id2value(id:Int) : T?
    abstract fun value2id(v:T): Int
}

abstract class ToggleButtonGroup<T> : MaterialButtonGroup() {
    private val internalMap = mutableMapOf<T,Boolean>()
    var selected:List<T>
        get() = internalMap.filter{it.value}.map {it.key}
        set(v) {
            owner?.clearChecked()
            v.forEach {
                owner?.check(value2id(it))
                internalMap[it] = true
            }
        }

    operator fun get(at:T):Boolean {
        return internalMap[at] ?: false
    }
    operator fun set(at:T, v:Boolean) {
        if(v) {
            owner?.check(value2id(at))
        } else {
            owner?.uncheck(value2id(at))
        }
        internalMap[at] = v
    }

    fun bind(gr: MaterialButtonToggleGroup, sourceToView:Boolean=true) {
        utAssert(!gr.isSingleSelection)

        owner = gr
        if(sourceToView) {
            internalMap.forEach { if(it.value) gr.check(value2id(it.key)) else gr.uncheck(value2id(it.key))}
        } else {
            val checked = gr.checkedButtonIds
            internalMap.keys.forEach { internalMap[it] = false }
            checked.forEach { internalMap[id2value(it)!!] = true}
        }
    }

    override fun onChecked(id: Int, checked: Boolean) {
        internalMap[id2value(id)!!] = checked
    }

    abstract fun id2value(id:Int) : T?
    abstract fun value2id(v:T): Int
}


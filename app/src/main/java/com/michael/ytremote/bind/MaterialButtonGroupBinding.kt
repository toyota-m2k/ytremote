@file:Suppress("unused")

package com.michael.bindit.impl

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButtonToggleGroup
import com.michael.bindit.BindingMode

abstract class MaterialButtonGroupBindingBase<T,DataType> (
    override val data:MutableLiveData<DataType>,
    mode:BindingMode = BindingMode.TwoWay
)  : BaseBinding<DataType>(mode), MaterialButtonToggleGroup.OnButtonCheckedListener {
    private var btnListener: MaterialButtonToggleGroup.OnButtonCheckedListener? = null

     lateinit var idResolver: IIDValueResolver<T>

     val toggleGroup:MaterialButtonToggleGroup?
        get() = view as? MaterialButtonToggleGroup

    open fun connect(owner:LifecycleOwner, view:MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        this.idResolver = idResolver
        super.connect(owner,view)
        if(mode!=BindingMode.OneWay) {
            view.addOnButtonCheckedListener(this)
        }
    }

    override fun cleanup() {
        if(mode!=BindingMode.OneWay) {
            toggleGroup?.removeOnButtonCheckedListener(this)
        }
        super.cleanup()
    }
}

/**
 * MaterialButtonToggleGroup を使ったラジオボタングループのバインディング
 * 考え方は RadioGroup と同じだが、i/fが異なるので、クラスは別になる。
 */
class MaterialRadioButtonGroupBinding<T>(
    data:MutableLiveData<T>,
    mode:BindingMode = BindingMode.TwoWay
) : MaterialButtonGroupBindingBase<T,T>(data,mode) {

    override fun connect(owner: LifecycleOwner, view: MaterialButtonToggleGroup, idResolver: IIDValueResolver<T>) {
        view.isSingleSelection = true
        super.connect(owner, view, idResolver)
    }

    // View --> Source
    override fun onButtonChecked(group: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean) {
        if(isChecked) {
            val v = idResolver.id2value(checkedId)
            if(data.value!=v) {
                data.value = v
            }
        }
    }

    // Source --> View
    override fun onDataChanged(v: T?) {
        val view = toggleGroup?:return
        if(v!=null) {
            val id = idResolver.value2id(v)
            if(view.checkedButtonId != id) {
                view.clearChecked()
                view.check(id)
            }
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<T>, idResolver: IIDValueResolver<T>, mode:BindingMode = BindingMode.TwoWay) : MaterialRadioButtonGroupBinding<T> {
            return MaterialRadioButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

/**
 * MaterialButtonToggleGroup を使ったトグルボタングループのバインディング
 * 各トグルボタンにT型のユニークキー（enumかR.id.xxxなど）が１：１に対応しているとして、そのListで選択状態をバインドする。
 * MaterialButtonToggleGroupを使う場合、トグルボタンとしてButtonを使うため、個々のボタンの選択状態の指定や選択イベントは使えないので。
 */
class MaterialToggleButtonGroupBinding<T>(
    data:MutableLiveData<List<T>>,
    mode:BindingMode = BindingMode.TwoWay
) : MaterialButtonGroupBindingBase<T,List<T>>(data,mode) {

    private val selected = mutableSetOf<T>()
    private var busy = false
    private fun inBusy(fn:()->Unit) {
        if(!busy) {
            busy = true
            try {
                fn()
            } finally {
                busy = false
            }
        }
    }

    override fun onDataChanged(v: List<T>?) {
        val view = toggleGroup ?: return
        inBusy {
            view.clearChecked()
            selected.clear()
            if (!v.isNullOrEmpty()) {
                selected.addAll(v)
                v.forEach {
                    view.check(idResolver.value2id(it))
                }
            }
        }
    }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        inBusy {
            val v = idResolver.id2value(checkedId) ?: return@inBusy
            if(isChecked) {
                selected.add(v)
            } else {
                selected.remove(v)
            }
            data.value = selected.toList()
        }
    }

    companion object {
        fun <T> create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, data:MutableLiveData<List<T>>, idResolver: IIDValueResolver<T>, mode:BindingMode = BindingMode.TwoWay) : MaterialToggleButtonGroupBinding<T> {
            return MaterialToggleButtonGroupBinding(data, mode).apply { connect(owner,view,idResolver) }
        }
    }
}

/**
 * MaterialButtonToggleGroupに支配される、複数のボタンと、その選択状態(LiveData<Boolean>)を個々にバインドするクラス。
 *
 * Usage:
 * 
 * val binding = MaterialToggleButtonsBinding(owner,toggleGroup).apply {
 *               add(button1, viewModel.toggle1)
 *               add(button2, viewModel.toggle2)
 *               ...
 *            }
 */
class MaterialToggleButtonsBinding (
    override val mode:BindingMode = BindingMode.TwoWay
) : DisposableImpl(),  MaterialButtonToggleGroup.OnButtonCheckedListener {

    var toggleGroup:MaterialButtonToggleGroup? = null


    private inner class DataObserver(owner:LifecycleOwner, val button: View, val data:MutableLiveData<Boolean>) : Observer<Boolean> {
        init {
            if (mode != BindingMode.OneWayToSource) {
                data.observe(owner,this)
            }
        }

        fun dispose() {
            data.removeObserver(this)
        }

        override fun onChanged(t: Boolean?) {
            val view = toggleGroup ?: return
            val cur = view.checkedButtonIds.contains(button.id)
            if(t==true) {
                if(!cur) {
                    view.check(button.id)
                }
            } else {
                if(cur) {
                    view.uncheck(button.id)
                }
            }
        }
    }

//    private val weakOwner = WeakReference(owner)
//    private val owner:LifecycleOwner?
//        get() = weakOwner.get()
    private val buttons = mutableMapOf<Int,DataObserver>()

    fun connect(view: MaterialButtonToggleGroup) {
        toggleGroup = view
        if(mode!=BindingMode.OneWay) {
            view.addOnButtonCheckedListener(this)
        }
    }

    data class ButtonAndData(val button:View, val data:MutableLiveData<Boolean>)

    fun add(owner:LifecycleOwner, button:View, data:MutableLiveData<Boolean>):MaterialToggleButtonsBinding {
        buttons[button.id] = DataObserver(owner,button,data)
        return this
    }

    fun add(owner:LifecycleOwner, vararg buttons:ButtonAndData):MaterialToggleButtonsBinding {
        for(b in buttons) {
            add(owner, b.button, b.data)
        }
        return this
    }

    override fun cleanup() {
        if (mode != BindingMode.OneWayToSource) {
            buttons.forEach { (_, data) ->
                data.dispose()
            }
        }
        buttons.clear()
        if(mode!=BindingMode.OneWay) {
            toggleGroup?.removeOnButtonCheckedListener(this)
        }
    }

    override fun onButtonChecked(
        group: MaterialButtonToggleGroup?,
        checkedId: Int,
        isChecked: Boolean
    ) {
        val v = buttons[checkedId] ?: return
        if(v.data.value != isChecked) {
            v.data.value = isChecked
        }
    }
    companion object {
        fun create(view:MaterialButtonToggleGroup, mode:BindingMode = BindingMode.TwoWay) : MaterialToggleButtonsBinding {
            return MaterialToggleButtonsBinding(mode).apply { connect(view) }
        }
        fun create(owner: LifecycleOwner, view:MaterialButtonToggleGroup, mode:BindingMode, vararg buttons:ButtonAndData) : MaterialToggleButtonsBinding {
            return MaterialToggleButtonsBinding(mode).apply {
                connect(view)
                add(owner, *buttons)
            }
        }
    }
}
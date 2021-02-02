package com.michael.ytremote.utils

import android.widget.RadioGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButtonToggleGroup
import java.lang.ref.WeakReference

/**
 * MaterialButtonToggleGroup を使ってButtonやMaterialButtonをトグルボタン的に使う場合に、バインドがうまくいかない（※）。
 * これをプログラム的になんとかするための、ちょっとした仕掛け。
 * Button/MaterialButtonには checked 属性がなく、MaterialButtonToggleGroupが、checkedButtonId(s) を持って、
 * その背景色などを変化させることで、トグルボタン的に見せている模様。
 * MaterialButtonToggleGroupのcheckedButtonに　ButtonIdをバインドすることになるのだが、中のボタンがCheckButtonでないとエラーになる。
 * BindingAdapterを定義すればよい、みたいな記事にしたがって試したが、なんかうまくいかないし、うまくいったとしても、
 * Enum <-> ButtonId <-> Bind みたいになって、バインドしても、全然幸せになれる感じがしないので、（多少の敗北感は否めないが）プログラム的に解決してしまった。
 */

/**
 * MaterialButtonToggleGroup を扱うための基底クラス
 */
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

/**
 * MaterialButtonToggleGroupをラジオボタン（単一選択）的に使うためのヘルパークラス
 */
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

/**
 * MaterialButtonToggleGroupをトグルボタン（複数選択可能）として使うためのヘルパークラス
 */
abstract class ToggleButtonGroup<T> : MaterialButtonGroup() {
    private val selectionSet = mutableSetOf<T>()
    var selected:List<T>
        get() = selectionSet.toList()
        set(v) {
            owner?.clearChecked()
            selectionSet.clear()
            v.forEach {
                owner?.check(value2id(it))
                selectionSet.add(it)
            }
        }

    operator fun get(at:T):Boolean {
        return selectionSet.contains(at)
    }

    operator fun set(at:T, v:Boolean) {
        if(v) {
            owner?.check(value2id(at))
            selectionSet.add(at)
        } else {
            owner?.uncheck(value2id(at))
            selectionSet.remove(at)
        }
    }

    fun bind(gr: MaterialButtonToggleGroup, sourceToView:Boolean=true) {
        utAssert(!gr.isSingleSelection)

        owner = gr
        if(sourceToView) {
            val list = selectionSet.toList()
            gr.clearChecked()
            selected = list
        } else {
            selectionSet.clear()
            gr.checkedButtonIds.forEach {
                selectionSet.add(id2value(it) ?: return@forEach)
            }
        }
    }

    override fun onChecked(id: Int, checked: Boolean) {
        val v = id2value(id) ?: return
        if(checked) {
            selectionSet.add(v)
        } else {
            selectionSet.remove(v)
        }
    }

    abstract fun id2value(id:Int) : T?
    abstract fun value2id(v:T): Int
}


/**
 * （Materialでない）RadioGroupを、RadioButtonGroup互換的に使うためのクラス
 * こちらは、普通にバインドできると思っていたけど、ViewModel <-> Enum <-> ButtonId <-> RadioGroup
 */

abstract class RawRadioButtonGroup<T>()  {
    private var wrToggleGroup: WeakReference<RadioGroup>? = null
    var owner: RadioGroup?
        get() = wrToggleGroup?.get()
        protected set(v) {
            if(v!=null) {
                wrToggleGroup = WeakReference(v)
                v.setOnCheckedChangeListener { group, checkedId ->
                    onChecked(checkedId)
                }
            }
        }
    
    private val internalSelectedValue = MutableLiveData<T>()
    val selectedValue: LiveData<T> = internalSelectedValue
    var selected:T?
        get() = internalSelectedValue.value
        set(v) {
            if(v!=null) {
                owner?.check(value2id(v))
            } else if(internalSelectedValue.value!=null) {
                owner?.clearCheck()
            }
            internalSelectedValue.value = v
        }

    fun bind(gr: RadioGroup, sourceToView:Boolean=true) {
        owner = gr
        if(sourceToView) {
            val sel = selected
            gr.clearCheck()
            if(sel!=null) {
                gr.check(value2id(sel))
            }
        } else {
            internalSelectedValue.value = id2value(gr.checkedRadioButtonId)
        }
    }

    fun onChecked(id: Int) {
        internalSelectedValue.value = id2value(id)
    }

    abstract fun id2value(id:Int) : T?
    abstract fun value2id(v:T): Int
}

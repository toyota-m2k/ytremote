package com.michael.ytremote.utils

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods
import com.google.android.material.button.MaterialButton


@InverseBindingMethods(InverseBindingMethod(type = MaterialButton::class, attribute = "android:checked"))
object MaterialButtonBindingAdapter {
    @BindingAdapter("android:checked")
    fun setChecked(view: MaterialButton, checked: Boolean) {
        if (view.isChecked != checked) {
            view.isChecked = checked
        }
    }

    @BindingAdapter(value = ["android:checkedAttrChanged"], requireAll = false)
    fun setListeners(view: MaterialButton, attrChange: InverseBindingListener?) {
        if (attrChange != null) {
            // TODO:
            //   丁寧に実装するには、TextViewBindingAdapterのようにListenerUtilというクラスを使って
            //   前回仕掛けたリスナーを明示的に解除する必要がある。
            //   参考: https://android.googlesource.com/platform/frameworks/data-binding/+/master/extensions/baseAdapters/src/main/java/android/databinding/adapters/TextViewBindingAdapter.java
            view.clearOnCheckedChangeListeners()
            view.addOnCheckedChangeListener { buttonView, isChecked -> attrChange.onChange() }
        }
    }
}
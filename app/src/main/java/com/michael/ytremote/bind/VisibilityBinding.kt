package com.michael.bindit.impl

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.michael.bindit.BindingMode
import com.michael.bindit.BoolConvert

@Suppress("unused")
open class VisibilityBinding(
        rawData: LiveData<Boolean>,
        boolConvert: BoolConvert = BoolConvert.Staright,
        private val hiddenMode:HiddenMode = HiddenMode.HideByGone
) : BaseBinding<Boolean>(BindingMode.OneWay) {
    enum class HiddenMode {
        HideByGone,
        HideByInvisible,
    }

    override val data: LiveData<Boolean> = if(boolConvert==BoolConvert.Inverse) rawData.map { !it } else rawData

    override fun onDataChanged(v: Boolean?) {
        val view = this.view ?: return
        view.visibility = when {
            v==true -> View.VISIBLE
            hiddenMode==HiddenMode.HideByGone -> View.GONE
            else -> View.INVISIBLE
        }
    }

    companion object {
        fun create(owner: LifecycleOwner, view: View, data: LiveData<Boolean>, boolConvert: BoolConvert = BoolConvert.Staright, hiddenMode:HiddenMode = HiddenMode.HideByGone) : VisibilityBinding {
            return VisibilityBinding(data, boolConvert, hiddenMode).apply { connect(owner, view) }
        }
    }
}
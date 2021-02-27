@file:Suppress("unused")

package com.michael.bindit.impl

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.michael.bindit.util.Callback
import com.michael.bindit.util.Listeners

class ClickBinding<V>(
    owner:LifecycleOwner,
    val view: V,
    fn:((View)->Unit)
    ) : View.OnClickListener where V:View {
    @Suppress("MemberVisibilityCanBePrivate")
    val listeners = Listeners<View>()
    init {
        view.setOnClickListener(this)
        listeners.add(owner,fn)
    }

    override fun onClick(v: View?) {
        listeners.invoke(view)
    }
}

class LongClickBinding<V>(
    owner: LifecycleOwner,
    val view: V,
    fn: (V)->Boolean
) : View.OnLongClickListener where V:View {
    @Suppress("MemberVisibilityCanBePrivate")
    val callback = Callback(owner,fn)
    init {
        view.setOnLongClickListener(this)
    }

    override fun onLongClick(v: View?): Boolean {
        return callback.invoke(view) ?: false
    }
}
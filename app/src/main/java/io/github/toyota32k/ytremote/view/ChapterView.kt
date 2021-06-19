package com.michael.ytremote.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import io.github.toyota32k.ytremote.model.AppViewModel
import java.util.jar.Attributes

class ChapterView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null, defStyleAttr:Int=0) : View(context, attrs, defStyleAttr) {
    private var mWidth:Int = 0

    init {
        val avm = AppViewModel.instance
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

    }
}
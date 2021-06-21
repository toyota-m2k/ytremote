package com.michael.ytremote.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.github.toyota32k.ytremote.data.ChapterList
import io.github.toyota32k.ytremote.model.AppViewModel
import java.util.jar.Attributes

class ChapterView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null, defStyleAttr:Int=0) : View(context, attrs, defStyleAttr) {
    private var mWidth:Int = 0
    private var mHeight:Int = 0

    private var duration:Long = 0L
    private var chapterList:ChapterList? = null

    fun setChapterList(list:ChapterList?, dur:Long) {
        duration = dur
        chapterList = list
    }

    init {
        val avm = AppViewModel.instance
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if(canvas==null) return
        if(mWidth==0||mHeight==0) return

        // background
        val rcBounds = Rect(0,0,mWidth,mHeight)
        val paint = Paint()
        paint.setColor(Color.rgb(0,0,0x8b))
        canvas.drawRect(rcBounds,paint)

        // chapters
        val list = chapterList ?: return

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if(w>0 && h>=0 && (mWidth!=w || mHeight!=h)) {
            mWidth = w
            mHeight = h
            invalidate()
        }
    }
}
package com.michael.ytremote.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.github.toyota32k.ytremote.data.ChapterList
import io.github.toyota32k.ytremote.model.AppViewModel
import io.github.toyota32k.ytremote.player.PlayerStateModel
import io.github.toyota32k.ytremote.player.Range


class ChapterView @JvmOverloads constructor(context: Context, attrs: AttributeSet?=null, defStyleAttr:Int=0) : View(context, attrs, defStyleAttr) {
    private var mWidth:Int = 0
    private var mHeight:Int = 0

    private var duration:Long = 0L
    private var chapterList:ChapterList? = null
    private var disabledRanges:List<Range>? = null

    private val mTickWidth = 1f

    fun setChapterList(ci:PlayerStateModel.ChapterInfo?) { // list:ChapterList?, dur:Long, trimming:Range) {
        if(ci==null) {
            val needsRedraw = duration!=0L
            duration = 0
            chapterList = null
            disabledRanges = null
            if(needsRedraw) {
                invalidate()
            }
        } else {
            duration = ci.duration
            chapterList = ci.list
            disabledRanges = ci.list.disabledRanges(ci.trimming).toList()
            invalidate()
        }
    }

    private fun time2x(time: Long): Float {
        return if (duration == 0L) 0f else mWidth.toFloat() * time.toFloat() / duration.toFloat()
    }

    val rect = RectF()
    val paint = Paint()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if(canvas==null) return
        if(mWidth==0||mHeight==0) return

        val width = mWidth.toFloat()
        val height = mHeight.toFloat()
        // background
        rect.set(0f,0f, width, height)
        paint.setColor(Color.rgb(0,0,0x8b))
        canvas.drawRect(rect,paint)

        // chapters
        if(duration<=0L) return
        val list = chapterList ?: return
        paint.setColor(Color.WHITE)
        for(c in list) {
            val x = time2x(c.position)
            rect.set(x,0f,x+mTickWidth,height)
            canvas.drawRect(rect,paint)
        }

        // disabled range
        val dr = disabledRanges
        if(dr.isNullOrEmpty()) return

        rect.set(0f,height/2, width, height)
        paint.setColor(Color.rgb(0x80,0xFF, 0))
        canvas.drawRect(rect,paint)

        paint.setColor(Color.GRAY)
        for(r in dr) {
            val end = if (r.end == 0L) duration else r.end
            val x1 = time2x(r.start)
            val x2 = time2x(end)
            rect.set(x1,height/2f,x2,height)
            canvas.drawRect(rect,paint)
        }
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
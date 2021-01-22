package com.michael.ytremote.player

import java.lang.Long.max
import java.lang.Long.min

data class MicClipping (val start:Long, val end:Long=0) {
    val isValid
        get() = start>0 || end>0

    /**
     * pos が start-end 内に収まるようクリップする
     */
    fun clipPos(pos:Long) : Long {
        return if(end>start) {
            min(max(start, pos), end)
        } else {
            max(start, pos)
        }
    }

    companion object {
        val empty = MicClipping(0,0)
    }
}

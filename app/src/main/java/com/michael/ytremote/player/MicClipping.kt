package com.michael.ytremote.player

import java.lang.Long.max
import java.lang.Long.min

data class MicClipping (val start:Long, val end:Long=-1) {
    val isValid
        get() = end>start

    fun clipPos(pos:Long) : Long {
        return if(end>start) {
            min(max(start, pos), end)
        } else {
            max(start, pos)
        }
    }
}

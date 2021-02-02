package com.michael.ytremote.utils

import com.michael.ytremote.BuildConfig
import org.json.JSONArray


fun <T> List<T>.reverse():Iterable<T> {
    return Iterable {
        iterator {
            for (i in count()-1 downTo 0) {
                yield(get(i)!!)
            }
        }
    }
}

fun utAssert(f:Boolean, msg:(()->String?)?=null) {
    if (BuildConfig.DEBUG && !f) {
        error(msg?.invoke() ?: "Assertion failed")
    }

}
package com.michael.ytremote.utils

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

package com.michael.ytremote.data

import java.lang.StringBuilder

enum class Rating(val v:Int) {
    DREADFUL(1),
    BAD(2),
    NORMAL(3),
    GOOD(4),
    EXCELLENT(5),
}

enum class Mark(val v:Int) {
    MARK_NONE(0),
    MARK_STAR(1),
    MARK_FLAG(2),
    MARK_HEART(3),
}

enum class SourceType(val v:Int) {
    SOURCE_DB(0),
    SOURCE_LISTED(1),
    SOURCE_SELECTED(2),
}

data class VideoItemFilter(val rating:Rating?=null, val mark:Mark?=null, val category:String?=null) {
    private fun getQueryString():String {
        val qb = QueryBuilder()
        if(rating!=null) {
            qb.add("r", rating.v)
        }
        if(mark!=null) {
            qb.add("m", mark.v)
        }
        if(category!=null) {
            qb.add("c", category)
        }
        return qb.queryString
    }

    fun urlWithQueryString(url:String) : String {
        val query = getQueryString()
        return if(query.isNotEmpty()) {
            "${url}?${query}"
        } else {
            url
        }
    }

}
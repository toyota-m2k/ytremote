package com.michael.ytremote.data

import com.michael.ytremote.R
import java.lang.StringBuilder

enum class Rating(val v:Int, val id:Int) {
    DREADFUL(1, R.id.tg_rating_dreadful),
    BAD(2, R.id.tg_rating_bad),
    NORMAL(3, R.id.tg_rating_normal),
    GOOD(4, R.id.tg_rating_good),
    EXCELLENT(5, R.id.tg_rating_excellent),
}

enum class Mark(val v:Int, val id:Int) {
    MARK_NONE(0, 0),
    MARK_STAR(1, R.id.tg_mark_star),
    MARK_FLAG(2, R.id.tg_mark_flag),
    MARK_HEART(3, R.id.tg_mark_heart),
}

enum class SourceType(val v:Int, val id:Int) {
    SOURCE_DB(0, R.id.chk_src_db),
    SOURCE_LISTED(1, R.id.chk_src_listed),
    SOURCE_SELECTED(2, R.id.chk_src_selected),
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
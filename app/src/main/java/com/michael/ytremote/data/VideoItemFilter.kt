package com.michael.ytremote.data

import com.michael.ytremote.R
import java.lang.StringBuilder

enum class Rating(val v:Int, val id:Int) {
    DREADFUL(1, R.id.tg_rating_dreadful),
    BAD(2, R.id.tg_rating_bad),
    NORMAL(3, R.id.tg_rating_normal),
    GOOD(4, R.id.tg_rating_good),
    EXCELLENT(5, R.id.tg_rating_excellent);

    companion object {
        fun valueOf(v: Int, def: Rating = NORMAL): Rating {
            return values().find { it.v == v } ?: def
        }
    }
}

enum class Mark(val v:Int, val id:Int) {
    NONE(0, 0),
    STAR(1, R.id.tg_mark_star),
    FLAG(2, R.id.tg_mark_flag),
    HEART(3, R.id.tg_mark_heart);

    companion object {
        fun valueOf(v: Int, def: Mark = Mark.NONE): Mark {
            return Mark.values().find { it.v == v } ?: def
        }
    }
}

enum class SourceType(val v:Int, val id:Int) {
    DB(0, R.id.chk_src_db),
    LISTED(1, R.id.chk_src_listed),
    SELECTED(2, R.id.chk_src_selected);

    companion object {
        fun valueOf(v: Int, def: SourceType = DB): SourceType {
            return SourceType.values().find { it.v == v } ?: def
        }
    }
}

data class VideoItemFilter(val settings:Settings) {

    private fun getQueryString(date:Long):String {
        val qb = QueryBuilder()
        if(settings.sourceType!=SourceType.DB) {
            qb.add("s", settings.sourceType.v)
        }
        if(settings.rating!=Rating.NORMAL) {
            qb.add("r", settings.rating.v)
        }
        if(!settings.marks.isNullOrEmpty()) {
            qb.add("m", settings.marks.map{"${it.v}"}.joinToString("."))
        }
        if(!settings.category.isNullOrEmpty()) {
            qb.add("c", settings.category)
        }
        if(date>0) {
            qb.add("d","$date")
        }
        return qb.queryString
    }

    fun urlWithQueryString(date:Long) : String {
        val query = getQueryString(date)
        return if(query.isNotEmpty()) {
            "${settings.baseUrl}list?${query}"
        } else {
            "${settings.baseUrl}list"
        }
    }

}
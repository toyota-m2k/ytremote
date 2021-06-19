package io.github.toyota32k.ytremote.data

import io.github.toyota32k.bindit.IIDValueResolver
import io.github.toyota32k.ytremote.R

enum class Rating(val v:Int, val id:Int) {
    DREADFUL(1, R.id.tg_rating_dreadful),
    BAD(2, R.id.tg_rating_bad),
    NORMAL(3, R.id.tg_rating_normal),
    GOOD(4, R.id.tg_rating_good),
    EXCELLENT(5, R.id.tg_rating_excellent);

    private class IDResolver : IIDValueResolver<Rating> {
        override fun id2value(id: Int):Rating  = valueOf(id)
        override fun value2id(v: Rating): Int = v.id
    }
    companion object {
        fun valueOf(v: Int, def: Rating = NORMAL): Rating {
            return values().find { it.v == v } ?: def
        }
        val idResolver:IIDValueResolver<Rating> by lazy { IDResolver() }
    }
}

enum class Mark(val v:Int, val id:Int) {
    NONE(0, 0),
    STAR(1, R.id.tg_mark_star),
    FLAG(2, R.id.tg_mark_flag),
    HEART(3, R.id.tg_mark_heart);

    private class IDResolver : IIDValueResolver<Mark> {
        override fun id2value(id: Int): Mark = valueOf(id)
        override fun value2id(v: Mark): Int = v.id
    }

    companion object {
        fun valueOf(v: Int, def: Mark = Mark.NONE): Mark {
            return Mark.values().find { it.v == v } ?: def
        }
        val idResolver:IIDValueResolver<Mark> by lazy { IDResolver() }
    }
}

enum class SourceType(val v:Int, val id:Int) {
    DB(0, R.id.chk_src_db),
    LISTED(1, R.id.chk_src_listed),
    SELECTED(2, R.id.chk_src_selected);

    private class IDResolver : IIDValueResolver<SourceType> {
        override fun id2value(id: Int): SourceType = valueOf(id)
        override fun value2id(v: SourceType): Int = v.id
    }

    companion object {
        fun valueOf(v: Int, def: SourceType = DB): SourceType {
            return SourceType.values().find { it.v == v } ?: def
        }

        val idResolver:IIDValueResolver<SourceType> by lazy { IDResolver() }
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
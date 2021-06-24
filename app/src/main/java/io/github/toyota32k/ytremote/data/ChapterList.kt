package io.github.toyota32k.ytremote.data

import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.ytremote.model.AppViewModel
import io.github.toyota32k.ytremote.player.Range
import io.github.toyota32k.ytremote.utils.toIterable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class Chapter(val position:Long, val label:String, val skip:Boolean) {
    constructor(j:JSONObject):this(j.getLong("position"), j.getString("label"), j.getBoolean("skip"))
}

class ChapterList(val ownerId:String) : SortedList<Chapter, Long>(10, false,
    keyOf = {e->e.position},
    comparator = { x,y->
        val d = y-x
        if(d<0) -1 else if(d>0) 1 else 0 }) {

    private val position = Position()

    fun prev(current:Long) : Chapter? {
        find(current, position)
        return if(0<=position.prev&&position.prev<size) this[position.prev] else null
    }

    fun next(current:Long) : Chapter? {
        find(current, position)
        return if(0<=position.next&&position.next<size) this[position.next] else null
    }

    private fun disabledRangesRaw() = sequence<Range> {
        var skip = false
        var skipStart = 0L

        for (c in this@ChapterList) {
            if (c.skip) {
                if (!skip) {
                    skip = true
                    skipStart = c.position
                }
            } else {
                if (skip) {
                    skip = false
                    yield(Range(skipStart, c.position))
                }
            }
        }
        if(skip) {
            yield (Range(skipStart, 0))
        }

    }

    fun disabledRanges(trimming:Range) = sequence<Range> {
        var trimStart = trimming.start
        var trimEnd = trimming.end
        for (r in disabledRangesRaw()) {
            if (r.end < trimming.start) {
                // ignore
                continue
            } else if (trimStart > 0) {
                if (r.start < trimStart) {
                    yield(Range(0, r.end))
                    continue
                } else {
                    yield(Range(0, trimStart))
                }
                trimStart = 0
            }

            if (trimEnd > 0) {
                if (trimEnd < r.start) {
                    break
                } else if (trimEnd < r.end) {
                    trimEnd = 0
                    yield(Range(r.start, 0))
                    break
                }
            }
            yield(r)
        }
        if (trimStart > 0) {
            yield(Range(0, trimStart))
        }
        if (trimEnd > 0) {
            yield(Range(trimEnd, 0))
        }
    }

    companion object {
        suspend fun get(ownerId:String): ChapterList? {
            return try {
                val vm = AppViewModel.instance
                val url = vm.settings.urlChapters(ownerId)
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                val json = NetClient.executeAsync(req).use { res ->
                    if (res.code == 200) {
                        val body = withContext(Dispatchers.IO) {
                            res.body?.string()
                        } ?: throw IllegalStateException("Server Response No Data.")
                        JSONObject(body)
                    } else {
                        throw IllegalStateException("Server Response Error (${res.code})")
                    }
                }
                // kotlin の reduce は、accumulatorとelementの型が同じ場合（sumみたいなやつ）しか扱えない。というより、accの初期値が、iterator.next()になっているし。
                // 代わりに、fold を使うとうまくいくというハック情報。
                json.getJSONArray("chapters").toIterable().fold<Any,ChapterList>(ChapterList(ownerId)) { acc,c-> acc.apply{ add(Chapter(c as JSONObject)) } }
            } catch(e:Throwable) {
                UtLogger.stackTrace(e)
                null
            }
        }
    }

}


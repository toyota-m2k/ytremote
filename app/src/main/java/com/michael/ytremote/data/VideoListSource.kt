package com.michael.ytremote.data

import com.michael.ytremote.player.MicClipping
import com.michael.ytremote.utils.UtLogger
import com.michael.ytremote.utils.toIterable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

fun JSONObject.safeGetLong(key:String, defValue:Long) : Long {
    return try {
        this.getLong(key)
    }  catch (e:Throwable) {
        defValue
    }
}

data class VideoItem(val id:String,val name:String, val start:Long, val end:Long) {
    internal constructor(j:JSONObject) : this(j.getString("id"), j.getString("name"), j.safeGetLong("start", 0), j.safeGetLong("end", 0))
    val url:String
        get() = HostInfo.videoUrl(id)
    val clipping:MicClipping
        get() = MicClipping(start,end)
}

object VideoListSource {
//    private var listRetrieved:((List<VideoItem>)->Unit)? = null
//
//    var filter:VideoItemFilter? = null
//
//    @ExperimentalCoroutinesApi
//    val sourceFlow: Flow<List<VideoItem>> = callbackFlow {
//        offer(listOf())
//        listRetrieved = { list->
//            offer(list)
//        }
//        awaitClose {
//            listRetrieved = null
//        }
//    }
//
//    fun update() {
//        CoroutineScope(Dispatchers.Default).launch {
//            val list = retrieve(filter)
//            if(null!=list) {
//                listRetrieved?.invoke(list)
//            }
//        }
//    }

    public const val urlBase = "http://192.168.0.12:3500/ytplayer"

    private const val listUrl = "http://192.168.0.12:3500/ytplayer/list"

    suspend fun retrieve(filter:VideoItemFilter? = null) : List<VideoItem>? {
        val url = HostInfo.listUrl(filter)
        val req = Request.Builder()
                .url(url)
                .get()
                .build()

        return try {
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
            val jsonList = json.getJSONArray("list") ?: throw IllegalStateException("Server Response Null List.")
            jsonList.toIterable().map { j -> VideoItem(j as JSONObject) }
        } catch (e: Throwable) {
            UtLogger.stackTrace(e)
            return null
        }
    }
}
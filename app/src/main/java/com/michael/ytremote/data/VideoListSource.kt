package com.michael.ytremote.data

import com.michael.ytremote.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Dispatcher
import okhttp3.Request
import org.json.JSONObject
import java.lang.IllegalStateException

data class VideoItem(val id:String,val name:String) {
    internal constructor(j:JSONObject) : this(j.getString("id"), j.getString("name"))
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

    val listUrl = "http://localhost:3500/ytplayer/list"

    suspend fun retrieve(filter:VideoItemFilter? = null) : List<VideoItem>? {
        val url = filter?.urlWithQueryString(listUrl) ?: listUrl
        val req = Request.Builder()
                .url(url)
                .get()
                .build()

        return try {
            val json = NetClient.executeAsync(req).use { res ->
                if (res.code != 200) {
                    val body = withContext(Dispatchers.IO) {
                        res.body?.string()
                    } ?: throw IllegalStateException("Server Response No Data.")
                    JSONObject(body)
                } else {
                    throw IllegalStateException("Server Response Error")
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
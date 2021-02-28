package com.michael.ytremote.data

import com.michael.ytremote.BooApplication
import com.michael.ytremote.model.AppViewModel
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
        get() = AppViewModel.instance.settings.videoUrl(id)
    val clipping:MicClipping
        get() = MicClipping(start,end)
}

data class VideoListSource(val list:List<VideoItem>, val modifiedDate:Long) {
    suspend fun checkUpdate(date:Long) : Boolean {
        val url = AppViewModel.instance.settings.checkUrl(date)
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
            json.getString("update") == "1"
        } catch(e:Throwable) {
            UtLogger.stackTrace(e)
            return false
        }
    }

    companion object {
        suspend fun retrieve(date:Long=0L): VideoListSource? {
            val url = AppViewModel.instance.settings.listUrl(date)
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
                val lastUpdate = json.getString("date").toLong()
                val jsonList = json.getJSONArray("list")
                    ?: throw IllegalStateException("Server Response Null List.")
                VideoListSource( jsonList.toIterable().map { j -> VideoItem(j as JSONObject) }, lastUpdate )
            } catch (e: Throwable) {
                UtLogger.stackTrace(e)
                return null
            }
        }
    }
}
package com.michael.ytremote.data

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.michael.ytremote.BooApplication
import com.michael.ytremote.model.AppViewModel
import com.michael.ytremote.utils.utAssert

data class Settings(
    val activeHost: String?,
    val hostList: List<String>,
    val sourceType: SourceType,
    val rating:Rating,
    val marks:List<Mark>,
    val category:String?) {

    val isValid
        get() = !activeHost.isNullOrBlank()

    @Suppress("SpellCheckingInspection")
    val baseUrl : String
        get() = "http://${activeHost}:3500/ytplayer/"

    fun listUrl():String {
        return VideoItemFilter(this).urlWithQueryString()
    }

    fun videoUrl(id:String):String {
        return baseUrl + "video?id=${id}"
    }

    fun urlToRegister(url:String):String {
        return baseUrl + "register?url=${url}"
    }

    fun urlToListCategories(): String {
        return baseUrl + "category"
    }



    fun save(context: Context) {
        utAssert(isValid)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit {
            if(activeHost!=null) putString(KEY_ACTIVE_HOST, activeHost) else remove(KEY_ACTIVE_HOST)
            if(hostList.isNotEmpty()) putStringSet(KEY_HOST_LIST, hostList.toSet()) else remove(KEY_HOST_LIST)
            putInt(KEY_SOURCE_TYPE, sourceType.v)
            putInt(KEY_RATING, rating.v)
            putStringSet(KEY_MARKS, marks.map {it.toString()}.toSet())
            if(!category.isNullOrBlank()) putString(KEY_CATEGORY, category) else remove(KEY_CATEGORY)
        }
        AppViewModel.instance.settings = this
    }

    companion object {
        const val KEY_ACTIVE_HOST = "activeHost"
        const val KEY_HOST_LIST = "hostList"
        const val KEY_SOURCE_TYPE = "sourceType"
        const val KEY_RATING = "rating"
        const val KEY_MARKS = "marks"
        const val KEY_CATEGORY = "category"

        fun load(context: Context): Settings {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return Settings(
                    activeHost = pref.getString(KEY_ACTIVE_HOST, null),
                    hostList = pref.getStringSet(KEY_HOST_LIST, null)?.toList() ?: listOf(),
                    sourceType = SourceType.valueOf(pref.getInt(KEY_SOURCE_TYPE, -1)),
                    rating = Rating.valueOf(pref.getInt(KEY_RATING, -1)),
                    marks = pref.getStringSet(KEY_MARKS, null)?.map { Mark.valueOf(it) } ?: listOf(),
                    category = pref.getString(KEY_CATEGORY, null))
        }
    }
}
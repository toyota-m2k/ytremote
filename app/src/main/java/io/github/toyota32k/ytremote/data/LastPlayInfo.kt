package io.github.toyota32k.ytremote.data

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.toyota32k.utils.UtLog

data class LastPlayInfo(val id:String, val position:Long, val playing:Boolean) {
    companion object {
        const val KEY_LAST_PLAY_ID = "lastPlayId"
        const val KEY_LAST_PLAY_POSITION = "lastPlayPosition"
        const val KEY_LAST_PLAYING = "lastPlaying"

        fun get(context: Context) : LastPlayInfo? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val id = pref.getString(KEY_LAST_PLAY_ID, null) ?: return null
            val position = pref.getLong(KEY_LAST_PLAY_POSITION, 0L)
            val playing = pref.getBoolean(KEY_LAST_PLAYING, false)
            UtLog("Data").debug("load: $id, at=$position, playing=$playing")
            return LastPlayInfo(id, position, playing)
        }

        fun set(context: Context, id:String?, position:Long, playing:Boolean) {
            UtLog("Data").debug("save: $id, $position")
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            pref.edit {
                if(id!=null) putString(KEY_LAST_PLAY_ID, id) else remove(KEY_LAST_PLAY_ID)
                putLong(KEY_LAST_PLAY_POSITION, position)
                putBoolean(KEY_LAST_PLAYING, playing)
            }
        }
    }
}


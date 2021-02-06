package com.michael.ytremote.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.michael.ytremote.R


object PackageUtil {
    fun getPackageInfo(context:Context):PackageInfo? {
        return try {
            val name = context.packageName
            val pm = context.packageManager
            return pm.getPackageInfo(name, PackageManager.GET_META_DATA)
        } catch (e: Throwable) {
            UtLogger.stackTrace(e)
            null
        }

    }

    fun getVersion(context: Context):String? {
        return try {
            // バージョン番号の文字列を返す
            getPackageInfo(context)?.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            UtLogger.stackTrace(e)
            null
        }
    }

    fun appName(context: Context):String {
        return context.resources.getString(R.string.app_name)
    }
}
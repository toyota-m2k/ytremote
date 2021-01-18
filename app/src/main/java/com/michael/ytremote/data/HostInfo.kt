package com.michael.ytremote.data

object HostInfo {
    val hostAddress = "192.168.0.12"
    val urlBase = "http://${hostAddress}:3500/ytplayer/"

    private val listUrl = urlBase + "list"

    fun listUrl(filter:VideoItemFilter? = null):String {
        return filter?.urlWithQueryString(listUrl) ?: listUrl
    }

    fun videoUrl(id:String):String {
        return urlBase + "video?id=${id}"
    }
}
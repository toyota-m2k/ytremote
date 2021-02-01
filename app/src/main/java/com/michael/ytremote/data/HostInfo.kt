package com.michael.ytremote.data

data class HostInfo(val settings:Settings) {
//    val hostAddress = "192.168.0.12"
//    val urlBase = "http://${hostAddress}:3500/ytplayer/"
//
//    private val listUrl = urlBase + "list"

    fun listUrl():String {
        return VideoItemFilter(settings).urlWithQueryString()
    }

    fun videoUrl(id:String):String {
        return settings.baseUrl + "video?id=${id}"
    }
}
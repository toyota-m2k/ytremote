package com.michael.ytremote.data

import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object NetClient {
    val motherClient : OkHttpClient = OkHttpClient.Builder().build()

    fun newCall(req: Request):Call {
        return motherClient.newCall(req)
    }

    suspend fun executeAsync(req:Request):Response {
        return motherClient.newCall(req).executeAsync()
    }

    /**
     * Coroutineを利用し、スレッドをブロックしないで同期的な通信を可能にする拡張メソッド
     * OkHttpのnewCall().execute()を置き換えるだけで使える。
     */
    suspend fun Call.executeAsync() : Response {
        return suspendCoroutine {cont ->
            try {
                enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        cont.resume(response)
                    }
                })
            } catch(e:Throwable) {
                cont.resumeWithException(e)
            }
        }
    }

}
/**
 * ResetableEventの suspend 版
 * スレッドをブロックしないので、ResetableEventに比べて圧倒的に軽快な動作が期待できる（たぶん。しらんけど）。
 */
@file:Suppress("unused")

package io.github.toyota32k.ytremote.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendableEvent(private var signal:Boolean, private val autoReset:Boolean) {
    private val channel = Channel<Unit>(capacity = 1)
    private val mutex = Mutex()

    init {
        if(!signal) {
            runBlocking {
                channel.send(Unit)
                signal = false
            }
        }
    }

    suspend fun set() {
        mutex.withLock {
            if(!signal) {
                // 非シグナル --> シグナル状態にする
                signal = true
                channel.receive()
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            if(signal) {
                // シグナル --> 非シグナル
                signal = false
                channel.send(Unit)
            }
        }
    }

    suspend fun waitOne() {
        mutex.withLock {
            if(signal) {
                // すでにシグナル状態：オートリセットの場合はリセットしておく
                if(autoReset) {
                    channel.send(Unit)
                    signal = false
                }
                return
            }
        }
        // ここでイベントの待ち合わせ：channelに空きができるのを待つ
        channel.send(Unit)
        mutex.withLock {
            signal = if(autoReset) {
                false
            } else {
                // autoResetでない場合は、シグナル状態を維持するためチャネルを空にする
                channel.receive()
                true
            }
        }
    }

    suspend fun <T> withLock(action: () -> T): T {
        waitOne()
        return action()
    }
}
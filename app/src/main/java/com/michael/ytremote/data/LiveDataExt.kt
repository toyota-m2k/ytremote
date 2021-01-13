package com.michael.ytremote.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@ExperimentalCoroutinesApi
fun <T> LiveData<T>.asFlow(): Flow<T?> = callbackFlow {
    offer(value)
    val observer = Observer<T> {
        offer(it)
    }
    observeForever(observer)
    awaitClose { removeObserver(observer) }
}


package com.michael.ytremote.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

//fun <T,R> LiveData<T>.map(fn:(T)->R): LiveData<R>
//        = Transformations.map(this) { x->fn(x)}
//
//
//fun <T,R> LiveData<T>.flatMap(fn:(T)-> LiveData<R>): LiveData<R>
//        = Transformations.switchMap(this) { x->fn(x)}

fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> {
    var first = true;
    return MediatorLiveData<T>().also { med->
        med.addSource(this ) { current ->
            val prev = med.value
            if (first || (prev == null && current != null) || (prev != null && prev != current)) {
                first = false;
                med.value = current
            }
        }
    }
}

fun <T> LiveData<T>.filter(predicate:(T?)->Boolean): LiveData<T> {
    return MediatorLiveData<T>().also { med->
        med.addSource(this) { current ->
            if (predicate(current)) {
                med.value = current
            }
        }
    }
}

fun <T> LiveData<T>.notNull(): LiveData<T> {
    return MediatorLiveData<T>().also { med ->
        med.addSource(this) { current ->
            if (current != null) {
                med.value = current
            }
        }
    }
}

fun <T,T1,R> LiveData<T>.combineLatest(src2: LiveData<T1>, fn:(T?, T1?)->R?): LiveData<R> {
    val src = this
    return MediatorLiveData<R>().also { med ->
        med.addSource(src) { med.value = fn(it, src2.value) }
        med.addSource(src2) { med.value = fn(src.value, it) }
    }
}

fun <T,T1,T2, R> LiveData<T>.combineLatest(src2: LiveData<T1>, src3:LiveData<T2>, fn:(T?, T1?, T2?)->R?): LiveData<R> {
    val src = this
    return MediatorLiveData<R>().also { med ->
        med.addSource(src) { med.value = fn(it, src2.value, src3.value) }
        med.addSource(src2) { med.value = fn(src.value, it, src3.value) }
        med.addSource(src3) { med.value = fn(src.value, src2.value, it) }
    }
}

//fun <T,R> LiveData<T>.map(fn:(T?)->R?):LiveData<R> {
//    return MediatorLiveData<R>().also { med->
//        med.addSource(this) { med.value = fn(value) }
//    }
//}

@ExperimentalCoroutinesApi
fun <T> LiveData<T>.asFlow(): Flow<T?> = callbackFlow {
    offer(value)
    val observer = Observer<T> {
        offer(it)
    }
    observeForever(observer)
    awaitClose { removeObserver(observer) }
}


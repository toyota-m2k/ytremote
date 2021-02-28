@file:Suppress("unused")

package com.michael.ytremote.bind.list

import androidx.lifecycle.LifecycleOwner
import com.michael.bindit.util.ListenerKey
import com.michael.bindit.util.Listeners

class ObservableList<T> : MutableList<T> {
    companion object {
        fun <T> from (collection:Collection<T>): ObservableList<T> {
            return ObservableList<T>().apply {
                internalList = collection.toMutableList()
            }
        }
        fun <T> of(vararg e:T): ObservableList<T> {
            return ObservableList<T>().apply {
                internalList = mutableListOf(*e)
            }
        }
    }

    enum class MutationKind {
        REFRESH,
        REMOVE,
        INSERT,
        MOVE,
        CHANGED,
    }
    abstract class MutationEventData(val kind: MutationKind)
    class RefreshEventData : MutationEventData(MutationKind.REFRESH)
    data class RemoveEventData(val position:Int, val range:Int=1) :
        MutationEventData(MutationKind.REMOVE)
    data class InsertEventData(val position:Int, val range:Int=1) :
        MutationEventData(MutationKind.INSERT)
    data class ChangedEventData(val position:Int, val range:Int=1) :
        MutationEventData(MutationKind.CHANGED)
    data class MoveEventData(val from:Int, val to:Int) : MutationEventData(MutationKind.MOVE)

    private var mutationEvent = Listeners<MutationEventData>()

    fun addListener(owner:LifecycleOwner, fn:(MutationEventData)->Unit):ListenerKey {
        fn(RefreshEventData())
        return mutationEvent.add(owner,fn)
    }
    fun addListener(owner:LifecycleOwner, listener: Listeners.IListener<MutationEventData>): ListenerKey {
        listener.onChanged(RefreshEventData())
        return mutationEvent.add(owner,listener)
    }

    private var internalList: MutableList<T> = mutableListOf()

    override val size: Int
        get() = internalList.size

    override fun contains(element: T): Boolean {
        return internalList.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return internalList.containsAll(elements)
    }

    override fun get(index: Int): T {
        return internalList[index]
    }

    override fun indexOf(element: T): Int {
        return internalList.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return internalList.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return OLIterator()
    }

    override fun lastIndexOf(element: T): Int {
        return internalList.lastIndexOf(element)
    }

    override fun add(element: T): Boolean {
        return if(internalList.add(element)) {
            mutationEvent.invoke(InsertEventData(internalList.size-1))
            true
        } else {
            false
        }
    }

    override fun add(index: Int, element: T) {
        internalList.add(index, element)
        mutationEvent.invoke(InsertEventData(index))
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return if(internalList.addAll(index, elements)) {
            mutationEvent.invoke(InsertEventData(index, elements.size))
            true
        } else {
            false
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return if(internalList.addAll(elements)) {
            mutationEvent.invoke(InsertEventData(internalList.size-elements.size, elements.size))
            true
        } else {
            false
        }
    }

    override fun clear() {
        internalList.clear()
        mutationEvent.invoke(RefreshEventData())
    }

//    private open inner class MIterator:MutableIterator<T> {
//        protected var current = 0
//        override fun hasNext(): Boolean {
//            return internalList.size>current
//        }
//
//        override fun next(): T {
//            return internalList[current++]
//        }
//
//        override fun remove() {
//            current--
//            removeAt(current)
//        }
//    }
//
//    private open inner class MLIterator(initial:Int=0): MIterator(), MutableListIterator<T> {
//        init {
//            current = initial
//        }
//
//        override fun hasPrevious(): Boolean {
//            return current>0
//        }
//
//        override fun nextIndex(): Int {
//            return current
//        }
//
//        override fun previous(): T {
//            return internalList[--current]
//        }
//
//        override fun previousIndex(): Int {
//            return current-1
//        }
//
//        override fun add(element: T) {
//            add(current, element)
//        }
//
//        override fun set(element: T) {
//            set(current, element)
//        }
//    }

    private inner class OLIterator(initial:Int=0) : MutableListIterator<T> {
        private var current:Int = initial
        private var next:Int = initial
        private var prev:Int = initial-1

        override fun hasPrevious(): Boolean {
            return prev>=0
        }

        override fun hasNext(): Boolean {
            return next<size
        }


        override fun nextIndex(): Int {
            return next
        }

        override fun previousIndex(): Int {
            return prev
        }

        override fun previous(): T {
            next--
            current = prev--
            return internalList[current]
        }

        override fun next(): T {
            prev++
            current = next++
            return internalList[current]
        }


        override fun add(element: T) {
            add(current, element)
            prev++
            next++
            current++
        }

        override fun remove() {
            if(current>=0) {
                removeAt(current)
                current--
                next = current+1
                prev = current
            }
        }

        override fun set(element: T) {
            set(current, element)
        }

    }

    override fun listIterator(): MutableListIterator<T> {
        return OLIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return OLIterator(index)
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        if(index>=0) {
            removeAt(index)
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var r = false
        val itr = iterator()
        while(itr.hasNext()) {
            if(elements.contains(itr.next())) {
                itr.remove()
                r = true
            }
        }
        return r
    }

    override fun removeAt(index: Int): T {
        val r = internalList.removeAt(index)
        mutationEvent.invoke(RemoveEventData(index))
        return r
    }

    fun removeAt(index: Int, count:Int) {
        val itr = internalList.listIterator(index)
        for(i in 0 until count) {
            if(!itr.hasNext()) break
            itr.next()
            itr.remove()
        }
        mutationEvent.invoke(RemoveEventData(index,count))
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var r = false
        val itr = iterator()
        while(itr.hasNext()) {
            if(!elements.contains(itr.next())) {
                itr.remove()
                r = true
            }
        }
        return r
    }

    override fun set(index: Int, element: T): T {
        val r = internalList[index]
        internalList[index] = element
        mutationEvent.invoke(ChangedEventData(index))
        return r
    }

    fun replace(list:Collection<T>) {
        internalList = list.toMutableList()
        mutationEvent.invoke(RefreshEventData())
    }

    fun move(from:Int, to:Int) {
        val f = internalList.removeAt(from)
        internalList.add(to, f)
        mutationEvent.invoke(MoveEventData(from,to))
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return internalList.subList(fromIndex, toIndex)
    }

}
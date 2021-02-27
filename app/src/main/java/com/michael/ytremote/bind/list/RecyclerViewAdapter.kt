package com.michael.ytremote.bind.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.michael.bindit.Binder
import com.michael.bindit.util.ListenerKey
import io.reactivex.rxjava3.disposables.Disposable

class RecyclerViewAdapter {
    /**
     * ObservableList を使った RecyclerView.Adapter のベースクラス
     */
    abstract class Base<T, VH>(
        owner: LifecycleOwner,
        val list: ObservableList<T>
    ) : Disposable, RecyclerView.Adapter<VH>() where VH : RecyclerView.ViewHolder
    {

//            set(v) {
//                field = v
//                v.mutationEvent.add(owner, this::onListChanged)
//                notifyDataSetChanged()
//            }

        /**
         * 単に、this::onListChanged を渡したいだけなのだが、コンストラクタでこれをやると、
         * >> Leaking 'this' in constructor of non-final class Base
         * というワーニングが出るので、一枚ラッパをはさむ。
         */
        private inner class ListMutationListener {
            fun onListChanged(t: ObservableList.MutationEventData?) {
                this@Base.onListChanged(t)
            }
        }
        private val listMutationListener = ListMutationListener()
        private var listenerKey:ListenerKey? = list.addListener(owner, listMutationListener::onListChanged)

        // region Disposable i/f
        @MainThread
        override fun dispose() {
            listenerKey?.let {
                listenerKey = null
                it.dispose()
            }
        }

        override fun isDisposed(): Boolean {
            return listenerKey==null
        }

        // endregion

        // Observer i/f

        private fun onListChanged(t: ObservableList.MutationEventData?) {
            if (t == null) return
            when (t) {
                is ObservableList.ChangedEventData -> notifyItemRangeChanged(t.position, t.range)
                is ObservableList.MoveEventData -> notifyItemMoved(t.from, t.to)
                is ObservableList.RemoveEventData -> notifyItemRangeRemoved(t.position, t.range)
                is ObservableList.InsertEventData -> notifyItemRangeInserted(t.position, t.range)
                else -> notifyDataSetChanged()
            }
        }

        // endregion

        // region RecyclerView.Adapter

        override fun getItemCount(): Int {
            return list.size
        }

        abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

        abstract override fun onBindViewHolder(holder: VH, position: Int)

        // endregion
    }

    class Simple<T>(
        owner:LifecycleOwner,
        list: ObservableList<T>,
        private val itemViewLayoutId:Int,
        val bindView: (binder: Binder, view: View, item:T)->Unit
    ) : Base<T, Simple.SimpleViewHolder>(owner,list) {
        class SimpleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val binder = Binder()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(itemViewLayoutId, parent, false)
            return SimpleViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            holder.binder.reset()
            bindView(holder.binder, holder.itemView, list[position])
        }
    }

    class SimpleWithDataBinding<T,B>(
        owner:LifecycleOwner,
        list: ObservableList<T>,
        val createView:(parent:ViewGroup, viewType:Int)->B,
        val bind: (binding: B, item:T)->Unit
    ) : Base<T, SimpleWithDataBinding.SimpleViewHolder<B>>(owner,list) where B:ViewDataBinding {

        class SimpleViewHolder<B>(val binding: B): RecyclerView.ViewHolder(binding.root) where B:ViewDataBinding

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder<B> {
            return SimpleViewHolder(createView(parent,viewType))
        }

        override fun onBindViewHolder(holder: SimpleViewHolder<B>, position: Int) {
            bind(holder.binding, list[position])
        }
    }

}
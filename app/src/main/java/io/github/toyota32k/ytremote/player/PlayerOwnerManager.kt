package io.github.toyota32k.ytremote.player

import android.content.Context
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.toyota32k.ytremote.model.AppViewModel
import io.github.toyota32k.ytremote.model.IPlayerOwner
import java.lang.ref.WeakReference

/**
 * ExoPlayer の所有者を管理するためのクラス
 * AppViewModel のフィールドとして保持される
 */
class PlayerOwnerManager(val appViewModel: AppViewModel) {
    val playerState = PlayerStateModel(appViewModel)
    private val playerBridge = PlayerModelBridge(appViewModel, playerState)
    val player:SimpleExoPlayer? get() = playerBridge.player

    private var primaryOwner : WeakReference<IPlayerOwner>? = null
    private var secondaryOwner : WeakReference<IPlayerOwner>? = null

    fun detachOwner(owner: IPlayerOwner) {
        owner.ownerResigned()
        if(owner==secondaryOwner?.get()) {
            secondaryOwner = null
            primaryOwner?.get()?.ownerAssigned(player!!)
        }
    }

    fun attachPrimaryOwner(owner: IPlayerOwner) {
        primaryOwner = WeakReference(owner)
        if(secondaryOwner?.get()==null) {
            owner.ownerAssigned(player!!)
        }
    }
    fun attachSecondaryOwner(owner: IPlayerOwner) {
        secondaryOwner = WeakReference(owner)
        primaryOwner?.get()?.ownerResigned()
        owner.ownerAssigned(player!!)
    }

    fun preparePlayer(context: Context) {
        playerBridge.preparePlayer(context)
    }

    fun closePlayer() {
        playerBridge.closePlayer()
    }

}
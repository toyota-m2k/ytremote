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

    enum class PlayerOwnerPriority {
        PRIMARY,            // MainViewModel：アプリ実行中は常に存在し、FullscreenActivityが閉じたとき、自動的にスイッチする
        SECONDARY,          // FullscreenActivity: フルスクリーン/PinP中のみ所有権を奪い、これが閉じると所有権をPrimaryに戻す
    }

    private var primaryOwner : WeakReference<IPlayerOwner>? = null
    private var secondaryOwner : WeakReference<IPlayerOwner>? = null

    fun attachOwner(owner: IPlayerOwner, priority: PlayerOwnerPriority) {
        secondaryOwner?.get()?.ownerResigned()
        primaryOwner?.get()?.ownerResigned()
        when(priority) {
            PlayerOwnerPriority.PRIMARY ->primaryOwner = WeakReference(owner)
            PlayerOwnerPriority.SECONDARY ->secondaryOwner = WeakReference(owner)
        }
        owner.ownerAssigned(player!!)
    }
    fun detachOwner(owner: IPlayerOwner) {
        owner.ownerResigned()
        if(owner==secondaryOwner?.get()) {
            secondaryOwner = null
            primaryOwner?.get()?.ownerAssigned(player!!)
        }
    }

    fun attachPrimaryOwner(owner: IPlayerOwner) {
        attachOwner(owner, PlayerOwnerPriority.PRIMARY)
    }
    fun attachSecondaryOwner(owner: IPlayerOwner) {
        attachOwner(owner, PlayerOwnerPriority.SECONDARY)
    }

    fun preparePlayer(context: Context) {
        playerBridge.preparePlayer(context)
    }

    fun closePlayer() {
        playerBridge.closePlayer()
    }

}
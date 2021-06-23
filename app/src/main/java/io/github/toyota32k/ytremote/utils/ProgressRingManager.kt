package io.github.toyota32k.ytremote.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ProgressBar

/**
 * ProgressBar の表示/非表示のアニメーションをサポートするクラス
 */
class ProgressRingManager(val progressRing: ProgressBar) : Animation.AnimationListener {
    private var currentAnimation : Animation? = null

    private val fadeInAnim = AlphaAnimation(0f,1f).apply {
        duration = 3000
        setAnimationListener(this@ProgressRingManager)
    }

    private val fadeOutAnim =  AlphaAnimation(1f,0f).apply {
        duration = 200
        setAnimationListener(this@ProgressRingManager)
    }

    fun show() {
        if(currentAnimation == fadeOutAnim) {
            fadeOutAnim.cancel()
        } else if (null!=currentAnimation || progressRing.visibility== View.VISIBLE) {
            return
        }
        currentAnimation = fadeInAnim
        progressRing.startAnimation(fadeInAnim)
    }

    fun hide() {
        if(currentAnimation == fadeInAnim) {
            fadeInAnim.cancel()
        } else if (null!=currentAnimation || progressRing.visibility== View.INVISIBLE) {
            return
        }
        currentAnimation = fadeOutAnim
        progressRing.startAnimation(fadeOutAnim)
    }

    override fun onAnimationRepeat(animation: Animation?) {
    }

    override fun onAnimationStart(animation: Animation?) {
        if(currentAnimation === fadeInAnim) {
            progressRing.visibility = View.VISIBLE
        }
    }

    override fun onAnimationEnd(animation: Animation?) {
        if(currentAnimation === fadeOutAnim) {
            progressRing.visibility = View.INVISIBLE
        }
        currentAnimation = null
        progressRing.clearAnimation()
    }
}

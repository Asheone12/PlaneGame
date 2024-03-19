package com.muen.planegame

import android.graphics.Rect

object AppHelper {
    var widthSurface = 0
    var heightSurface = 0
    var bound = Rect() // 屏幕范围

    var isPause = false
    var isRunning = false

    const val SCORE_EVENT = "score"
    const val COLLI_EVENT = "collision"
    const val FIRE_EVENT = "firePower"
    const val BOMB_RESET_EVENT = "bomb_reset"
    const val BOMB_ADD_EVENT = "bomb_add"
}
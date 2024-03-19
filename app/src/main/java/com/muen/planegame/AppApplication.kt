package com.muen.planegame

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.jeremyliao.liveeventbus.BuildConfig
import com.jeremyliao.liveeventbus.LiveEventBus

class AppApplication: Application() {
    companion object{
       @SuppressLint("StaticFieldLeak")
       lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
//        initLiveEvent();
    }

    private fun initLiveEvent() {
        LiveEventBus
            .config()
            .enableLogger(BuildConfig.DEBUG)
            .lifecycleObserverAlwaysActive(false)
    }
}
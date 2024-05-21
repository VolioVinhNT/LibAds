package com.volio.ads.admob.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdView
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.StateLoadAd
import java.util.*

abstract class AdmobAds {
    protected var timeLoader = 0L
    abstract fun isDestroy(): Boolean
    abstract fun isLoaded(): Boolean
    abstract fun getStateLoadAd(): StateLoadAd
    abstract fun destroy()
    abstract fun loadAndShow(
        activity: Activity,
        idAds: String,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    )

    abstract fun preload(activity: Activity, idAds: String)
    abstract fun show(
        activity: Activity,
        idAds: String,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean

    abstract fun setPreloadCallback(preloadCallback: PreloadCallback?)
    fun wasLoadTimeLessThanNHoursAgo(numHours: Long = 1): Boolean {
        val dateDifference: Long = Date().time - timeLoader
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    open fun getAdsView(): AdView? = null
}
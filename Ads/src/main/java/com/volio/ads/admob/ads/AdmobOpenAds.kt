package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*

class AdmobOpenAds : AdmobAds() {
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var error: String? = null

    private var loadFailed = false
    private var loaded: Boolean = false
    private var isTimeOut: Boolean = false
    private var preload: Boolean = false
    private var adsChild:AdsChild? = null


    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var currentActivity: Activity? = null


    val TAG = "AdmobOpenAds"
    override fun loadAndShow(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    ) {
        this.callback = adCallback
        currentActivity = activity
        preload = false
        load(activity,adsChild,loadingText,lifecycle,timeMillisecond?:Constant.TIME_OUT_DEFAULT,adCallback)
    }

    override fun preload(activity: Activity, adsChild: AdsChild) {
        preload = true
        load(activity,adsChild)
    }

    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        adCallback: AdCallback?
    ): Boolean {
        this.callback = adCallback
        currentActivity = activity
        if(loaded&&appOpenAd != null){
            appOpenAd?.show(currentActivity, fullScreenContentCallback)
            return true
        }
        return false
    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        textLoading: String? = null,
        lifecycle: Lifecycle? = null,
        timeOut: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? = null
    ) {
        this.adsChild = adsChild
        this.callback = adCallback
        callback = adCallback
        timeClick = System.currentTimeMillis();
        val id = if (Constant.isDebug) Constant.ID_ADMOB_OPEN_APP_TEST else adsChild.adsId

        if (!preload) {
            handler.postDelayed(Runnable {
                if (!loaded) {
                    isTimeOut = true
                    error = "TimeOut"
                    if (eventLifecycle == Lifecycle.Event.ON_RESUME) {
                        AdDialog.getInstance().hideLoading()
                        lifecycle?.removeObserver(lifecycleObserver)
                        adCallback?.onAdFailToLoad(error)
                    }
                }
            }, timeOut)

            lifecycle?.addObserver(lifecycleObserver)
        }
        loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAppOpenAdLoaded(ad: AppOpenAd) {
                Log.d(TAG, "onAppOpenAdLoaded: ")
                appOpenAd = ad
                loaded = true
                timeLoader = Date().time
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                    appOpenAd?.show(currentActivity, fullScreenContentCallback)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
            }

            override fun onAppOpenAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "onAppOpenAdFailedToLoad: ")
                loadFailed = true
                error = loadAdError.message
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload &&!isTimeOut) {
                    adCallback?.onAdFailToLoad(loadAdError.message)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
            }
        }
        val request: AdRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            activity, id, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback
        )

    }

    private val lifecycleObserver = LifecycleEventObserver { source, event ->
        eventLifecycle = event
        if (event == Lifecycle.Event.ON_RESUME) {
            AdDialog.getInstance().hideLoading()
            if (isTimeOut || loadFailed || loaded) {
                AdDialog.getInstance().hideLoading()
                if (loaded && appOpenAd != null) {
                    appOpenAd?.show(currentActivity, fullScreenContentCallback)
                } else {
                    callback?.onAdFailToLoad(error)
                }
            }
        }

    }
    private val fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            appOpenAd = null
            callback?.onAdClose(AdDef.ADS_TYPE.OPEN_APP)

            Log.d(TAG, "onAdDismissedFullScreenContent: ")
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            callback?.onAdFailToLoad(adError.message)
            Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
        }

        override fun onAdShowedFullScreenContent() {
            Log.d(TAG, "onAdShowedFullScreenContent: ")
            Utils.showToastDebug(currentActivity, "Admob Interstitial id: ${adsChild?.adsId}")

            callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.OPEN_APP)
        }
    }

    override fun isDestroy(): Boolean {
        return appOpenAd == null
    }

    override fun isLoaded(): Boolean {
        return loaded
    }

    override fun destroy() {
        appOpenAd = null
    }
}
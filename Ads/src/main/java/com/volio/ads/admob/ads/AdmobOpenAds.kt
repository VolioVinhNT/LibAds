package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.*
import java.util.*

class AdmobOpenAds : AdmobAds() {
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private var error: String? = null
    private var isTimeOut = false
    private var handler = Handler(Looper.getMainLooper())
    private var loadFailed = false
    private var loaded: Boolean = false
    private var preload: Boolean = false
    private var adsChild: AdsChild? = null


    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var lifecycle:Lifecycle? = null

    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE
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
        load(
            activity,
            adsChild,
            loadingText,
            lifecycle,
            timeMillisecond ?: Constant.TIME_OUT_DEFAULT,
            adCallback
        )
    }

    override fun preload(activity: Activity, adsChild: AdsChild) {
        preload = true
        load(activity, adsChild)
    }

    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean {
        this.callback = adCallback
        currentActivity = activity
        if (loaded && appOpenAd != null) {
            appOpenAd?.show(activity)
            return true
        }
        return false
    }
    private val timeOutCallBack = Runnable {
        if (!loaded && !loadFailed) {
            isTimeOut = true
            if (eventLifecycle == Lifecycle.Event.ON_RESUME){
                callback?.onAdFailToLoad("TimeOut")
                lifecycle?.removeObserver(lifecycleObserver)
            }
        }
    }
    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        textLoading: String? = null,
        lifecycle: Lifecycle? = null,
        timeOut: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? = null
    ) {
        this.lifecycle = lifecycle
        this.adsChild = adsChild
        this.callback = adCallback
        stateLoadAd = StateLoadAd.LOADING
        timeClick = System.currentTimeMillis();
        val id = if (Constant.isDebug) Constant.ID_ADMOB_OPEN_APP_TEST else adsChild.adsId
        if (!preload) {
            lifecycle?.addObserver(lifecycleObserver)
            handler.removeCallbacks(timeOutCallBack)
            handler.postDelayed(timeOutCallBack,timeOut)
        }
        resetValue()
        val openAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(p0: AppOpenAd) {
                Log.d(TAG, "onAdLoaded: ")
                appOpenAd = p0
                appOpenAd?.onPaidEventListener = OnPaidEventListener {
                    kotlin.runCatching {
                        val params = Bundle()
                        params.putString("revenue_micros", it.valueMicros.toString())
                        params.putString("currency", it.currencyCode)
                        params.putString("precision_type", it.precisionType.toString())
                        params.putString("ad_unit_id", p0.adUnitId)
                        params.putString("network", p0.responseInfo.mediationAdapterClassName)
                        callback?.onPaidEvent(params)
                    }
                }
                appOpenAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {

                        override fun onAdImpression() {
                            super.onAdImpression()
                            adCallback?.onAdImpression(AdDef.ADS_TYPE.OPEN_APP)
                            Firebase.analytics.logEvent(Constant.KeyCustomImpression, Bundle.EMPTY)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            Log.d(TAG, "onAdDismissedFullScreenContent: ")
                            callback?.onAdClose(AdDef.ADS_TYPE.OPEN_APP)
                            appOpenAd = null

                            //// perform your code that you wants to do after ad dismissed or closed
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            super.onAdFailedToShowFullScreenContent(adError)
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                            appOpenAd = null
                            loadFailed = true
                            error = adError.message
                            if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                                AdDialog.getInstance().hideLoading()
                                callback?.onAdFailToLoad(adError.message)
                                lifecycle?.removeObserver(lifecycleObserver)
                            }
                            /// perform your action here when ad will not load
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            callback?.onAdClick()
                            if (AdsController.mTopActivity != null && AdsController.mTopActivity is AdActivity) {
                                AdsController.mTopActivity?.finish()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.d(TAG, "onAdShowedFullScreenContent: ")
                            appOpenAd = null
                            AdDialog.getInstance().hideLoading()
                            Utils.showToastDebug(
                                activity,
                                "Admob OpenAds id: ${adsChild.adsId}"
                            )
                            stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
                            callback?.onAdShow(
                                AdDef.NETWORK.GOOGLE,
                                AdDef.ADS_TYPE.OPEN_APP
                            )
                        }
                    }
                if (!isTimeOut && eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        AdDialog.getInstance().hideLoading()
                    }, 500)
                    currentActivity?.let { appOpenAd?.show(it) }
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                loaded = true
                timeLoader = Date().time
                Log.d(TAG, "onAdLoaded: ")
                stateLoadAd = StateLoadAd.SUCCESS
                callbackPreload?.onLoadDone()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                loadFailed = true
                error = p0.message
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad(p0.message)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                stateLoadAd = StateLoadAd.FAILED
                callbackPreload?.onLoadFail()
            }
        }
        val request: AdRequest = AdRequest.Builder()
//            .setHttpTimeoutMillis(timeOut.toInt())
            .build()
        AppOpenAd.load(
            activity, id, request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, openAdLoadCallback
        )

    }

    private val lifecycleObserver = object :LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            eventLifecycle = event
            if (event == Lifecycle.Event.ON_RESUME) {
                AdDialog.getInstance().hideLoading()
                if (isTimeOut){
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad("TimeOut")
                    lifecycle?.removeObserver(this)
                } else if (loadFailed || loaded) {
                    AdDialog.getInstance().hideLoading()
                    if (loaded) {
                        currentActivity?.let { appOpenAd?.show(it) }
                    } else {
                        callback?.onAdFailToLoad(error)
                    }
                    lifecycle?.removeObserver(this)
                }
            }
        }
    }

    private fun resetValue() {
        loaded = false
        loadFailed = false
        error = null
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

    override fun getStateLoadAd(): StateLoadAd {
        return stateLoadAd
    }

    override fun setPreloadCallback(preloadCallback: PreloadCallback?) {
        callbackPreload = preloadCallback
    }
}
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
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.*
import java.util.*

class AdmobRewardInterstitial : AdmobAds() {
    private var error: String? = null

    private var loadFailed = false
    private var loaded: Boolean = false
    private var preload: Boolean = false
    private var isTimeOut = false
    private var handler = Handler(Looper.getMainLooper())
    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME
    private var rewardedAd: RewardedInterstitialAd? = null
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private val TAG = "AdmobInterstitial"
    private var currentActivity: Activity? = null
    private var lifecycle: Lifecycle? = null
    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE

    private fun resetValue() {
        loaded = false
        loadFailed = false
        error = null
    }

    override fun setPreloadCallback(preloadCallback: PreloadCallback?) {
        callbackPreload = preloadCallback
    }

    override fun loadAndShow(
        activity: Activity,
        idAds: String,
        textLoading: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    ) {
        callback = adCallback
        preload = false
        this.currentActivity = activity
        load(
            activity,
            idAds,
            textLoading,
            lifecycle,
            timeMillisecond ?: Constant.TIME_OUT_DEFAULT,
            adCallback
        )
    }

    override fun preload(activity: Activity, idAds: String) {
        preload = true
        load(activity, idAds, null, null, Constant.TIME_OUT_DEFAULT, null)
    }

    override fun show(
        activity: Activity,
        idAds: String,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean {
        this.currentActivity = activity
        this.callback = adCallback
        AdDialog.getInstance().showLoadingWithMessage(activity, loadingText)
        if (loaded && rewardedAd != null) {
            rewardedAd?.show(activity, rewardedAdLoadCallback)
            return true
        }
        return false
    }


    private val timeOutCallBack = Runnable {
        if (!loaded && !loadFailed) {
            isTimeOut = true
            if (eventLifecycle == Lifecycle.Event.ON_RESUME) {
                callback?.onAdFailToLoad("TimeOut")
                lifecycle?.removeObserver(lifecycleObserver)
            }
        }
    }

    private fun load(
        activity: Activity,
        idAds: String,
        textLoading: String?,
        lifecycle: Lifecycle?,
        timeOut: Long,
        adCallback: AdCallback?
    ) {
        this.lifecycle = lifecycle
        if (System.currentTimeMillis() - timeClick < 500) return
        textLoading?.let {
            AdDialog.getInstance().showLoadingWithMessage(activity, textLoading)
        }
        if (!preload) {
            lifecycle?.addObserver(lifecycleObserver)
            handler.removeCallbacks(timeOutCallBack)
            handler.postDelayed(timeOutCallBack, timeOut)
        }
        resetValue()
        stateLoadAd = StateLoadAd.LOADING
        callback = adCallback
        timeClick = System.currentTimeMillis();
        val id =
            if (Constant.isDebug) Constant.ID_ADMOB_REWARD_INTERSTITIAL_TEST else idAds
        Utils.showToastDebug(
            activity,
            "Admob ReWard Interstitial id: $idAds"
        )
        val rewardedAdLoadCallback = object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: RewardedInterstitialAd) {
                Log.d(TAG, "onAdLoaded: ")
                rewardedAd = p0
                loaded = true
                rewardedAd?.setOnPaidEventListener {
                    kotlin.runCatching {
                        val params = Bundle()
                        params.putString("revenue_micros", it.valueMicros.toString())
                        params.putString("precision_type", it.precisionType.toString())
                        params.putString("ad_unit_id", p0.adUnitId)
                        val adapterResponseInfo = p0.responseInfo.loadedAdapterResponseInfo
                        adapterResponseInfo?.let { it ->
                            params.putString("ad_source_id", it.adSourceId)
                            params.putString("ad_source_name", it.adSourceName)
                        }
                        callback?.onPaidEvent(params)
                    }
                }
                rewardedAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {

                        override fun onAdImpression() {
                            super.onAdImpression()
                            adCallback?.onAdImpression(AdDef.ADS_TYPE.REWARD_INTERSTITIAL)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            Log.d(TAG, "onAdDismissedFullScreenContent: ")
                            callback?.onAdClose(AdDef.ADS_TYPE.INTERSTITIAL)
                            rewardedAd = null

                            //// perform your code that you wants to do after ad dismissed or closed
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            super.onAdFailedToShowFullScreenContent(adError)
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                            rewardedAd = null
                            loadFailed = true
                            error = adError.message
                            if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                                AdDialog.getInstance().hideLoading()
                                callback?.onAdFailToLoad(adError.message)
                                lifecycle?.removeObserver(lifecycleObserver)
                            }
                            /// perform your action here when ad will not load
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.d(TAG, "onAdShowedFullScreenContent: ")
                            rewardedAd = null
                            AdDialog.getInstance().hideLoading()
//                            Utils.showToastDebug(
//                                activity,
//                                "Admob ReWard Interstitial id: ${adsChild.adsId}"
//                            )
//                            callback?.onAdShow(
//                                AdDef.NETWORK.GOOGLE,
//                                AdDef.ADS_TYPE.INTERSTITIAL
//                            )
                        }
                    }
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        AdDialog.getInstance().hideLoading()
                    }, 500)
                    currentActivity?.let { rewardedAd?.show(it, rewardedAdLoadCallback) }
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                timeLoader = Date().time
                stateLoadAd = StateLoadAd.SUCCESS
                callbackPreload?.onLoadDone()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
//                Utils.showToastDebug(
//                    activity,
//                    "Admob ReWard Interstitial id: ${adsChild.adsId}"
//                )
                loadFailed = true
                error = p0.message
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad(p0.message)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                stateLoadAd = StateLoadAd.FAILED
                callbackPreload?.onLoadFail()
            }
        }
        RewardedInterstitialAd.load(
            activity, id, AdRequest.Builder()
//            .setHttpTimeoutMillis(timeOut.toInt())
                .build(), rewardedAdLoadCallback
        )

    }

    private val rewardedAdLoadCallback = OnUserEarnedRewardListener {
        stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO)
        if (AdsController.adActivity != null && AdsController.adActivity is AdActivity) {
//            AdsController.adActivity?.finish()
        }
        //Utils.showToastDebug(currentActivity, "Admob ReWard Interstitial id: ${adsChild?.adsId}")
    }

//        override fun onRewardedAdClosed() {
//            super.onRewardedAdClosed()
//            callback?.onAdClose(AdDef.ADS_TYPE.REWARD_VIDEO)
//            Utils.showToastDebug(currentActivity, "Admob Interstitial id: ${adsChild?.adsId}")
//
//
//        }

    private val lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            eventLifecycle = event
            if (event == Lifecycle.Event.ON_RESUME) {
                AdDialog.getInstance().hideLoading()
                if (isTimeOut) {
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad("TimeOut")
                    lifecycle?.removeObserver(this)
                } else if (loadFailed || loaded) {
                    AdDialog.getInstance().hideLoading()
                    if (loaded) {
                        currentActivity?.let { rewardedAd?.show(it, rewardedAdLoadCallback) }
                    } else {
                        callback?.onAdFailToLoad(error)
                    }
                    lifecycle?.removeObserver(this)
                }
            }
        }
    }

    override fun isLoaded(): Boolean {
        return loaded
    }

    override fun isDestroy(): Boolean {
        return rewardedAd == null
    }

    override fun destroy() {

        isTimeOut = true
        rewardedAd = null
    }

    override fun getStateLoadAd(): StateLoadAd {
        return stateLoadAd
    }

}
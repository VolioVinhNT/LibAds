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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
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
    private var adsChild:AdsChild? = null
    private var lifecycle:Lifecycle? = null

    private fun resetValue() {
        loaded = false
        loadFailed = false
        error = null
    }

    override fun loadAndShow(
        activity: Activity,
        adsChild: AdsChild,
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
        load(activity, adsChild, textLoading, lifecycle, timeMillisecond?:Constant.TIME_OUT_DEFAULT, adCallback)
    }

    override fun preload(activity: Activity, adsChild: AdsChild) {
        preload = true
        load(activity, adsChild, null, null, Constant.TIME_OUT_DEFAULT, null)
    }

    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        adCallback: AdCallback?
    ): Boolean {
        this.currentActivity = activity
        this.callback = adCallback
        AdDialog.getInstance().showLoadingWithMessage(activity,loadingText)
        if (loaded && rewardedAd != null) {
            rewardedAd?.show(activity,rewardedAdLoadCallback)
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
        textLoading: String?,
        lifecycle: Lifecycle?,
        timeOut: Long,
        adCallback: AdCallback?
    ) {
        this.lifecycle = lifecycle
        this.adsChild = adsChild
        if (System.currentTimeMillis() - timeClick < 500) return
        textLoading?.let {
            AdDialog.getInstance().showLoadingWithMessage(activity, textLoading)
        }
        if (!preload) {
            lifecycle?.addObserver(lifecycleObserver)
            handler.removeCallbacks(timeOutCallBack)
            handler.postDelayed(timeOutCallBack,timeOut)
        }
        resetValue()
        callback = adCallback
        timeClick = System.currentTimeMillis();
        val id = if (Constant.isDebug) Constant.ID_ADMOB_REWARD_INTERSTITIAL_TEST else adsChild.adsId
        Utils.showToastDebug(
            activity,
            "Admob ReWard Interstitial id: ${adsChild.adsId}"
        )
        val rewardedAdLoadCallback = object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: RewardedInterstitialAd) {
                Log.d(TAG, "onAdLoaded: ")
                rewardedAd = p0
                rewardedAd?.setOnPaidEventListener {
                    kotlin.runCatching {
                        val params = Bundle()
                        params.putString("valuemicros", it.valueMicros.toString())
                        params.putString("currency", it.currencyCode)
                        params.putString("precision", it.precisionType.toString())
                        params.putString("adunitid", p0.adUnitId)
                        params.putString("network", p0.responseInfo.mediationAdapterClassName)
                        callback?.onPaidEvent(params)
                    }
                }
                rewardedAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
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
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload&&!isTimeOut) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        AdDialog.getInstance().hideLoading()
                    }, 500)
                    currentActivity?.let { rewardedAd?.show(it,rewardedAdLoadCallback) }
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                loaded = true
                timeLoader = Date().time
                Log.d(TAG, "onAdLoaded: ")
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
            }
        }
        RewardedInterstitialAd.load(activity,id,AdRequest.Builder()
//            .setHttpTimeoutMillis(timeOut.toInt())
            .build(),rewardedAdLoadCallback)

    }
    private val rewardedAdLoadCallback = OnUserEarnedRewardListener {
        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO)
        //Utils.showToastDebug(currentActivity, "Admob ReWard Interstitial id: ${adsChild?.adsId}")
    }

//        override fun onRewardedAdClosed() {
//            super.onRewardedAdClosed()
//            callback?.onAdClose(AdDef.ADS_TYPE.REWARD_VIDEO)
//            Utils.showToastDebug(currentActivity, "Admob Interstitial id: ${adsChild?.adsId}")
//
//
//        }

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
                        currentActivity?.let { rewardedAd?.show(it,rewardedAdLoadCallback) }
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

}
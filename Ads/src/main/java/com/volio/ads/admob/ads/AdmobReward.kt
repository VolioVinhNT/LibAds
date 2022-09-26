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
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.StateADCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.*
import java.util.*

class AdmobReward : AdmobAds() {
    private var error: String? = null

    private var loadFailed = false
    private var loaded: Boolean = false
    private var preload: Boolean = false
    private var isTimeOut = false
    private var handler = Handler(Looper.getMainLooper())
    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME
    private var rewardedAd: RewardedAd? = null
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private val TAG = "AdmobReward"
    private var currentActivity: Activity? = null
    private var adsChild:AdsChild? = null
    private var lifecycle:Lifecycle? = null
    private var stateLoadAd = StateLoadAd.NONE
    private var callbackPreload: PreloadCallback? = null

    private fun resetValue() {
        loaded = false
        loadFailed = false
        error = null
    }

    public fun setStateAdCallback(stateADCallback: StateADCallback?){
        stateADCallback?.onState(stateLoadAd)
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
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean {
        this.currentActivity = activity
        this.callback = adCallback
        AdDialog.getInstance().showLoadingWithMessage(activity,loadingText)
        if (loaded && rewardedAd != null) {
            stateLoadAd = StateLoadAd.NONE
            rewardedAd?.show(activity,rewardedAdLoadCallback)
            return true
        }
        return false
    }


    private val timeOutCallBack = Runnable {
        if (!loaded && !loadFailed) {
            isTimeOut = true
            if (eventLifecycle == Lifecycle.Event.ON_RESUME){
                stateLoadAd = StateLoadAd.FAILED
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
        stateLoadAd = StateLoadAd.LOADING
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
        Utils.showToastDebug(
            activity,
            "Admob ReWard id: ${adsChild.adsId}"
        )
        val id = if (Constant.isDebug) Constant.ID_ADMOB_REWARD_TEST else adsChild.adsId
        val rewardedAdLoadCallback = object : RewardedAdLoadCallback() {
            override fun onAdLoaded(p0: RewardedAd) {
                Log.d(TAG, "onAdLoaded: ")
                stateLoadAd = StateLoadAd.SUCCESS
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
                            stateLoadAd = StateLoadAd.NONE
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
                            Log.d(TAG, "onAdShowedFullScreenContent: ${callback==null}")
                            callback!!.onRewardShow(
                                AdDef.NETWORK.GOOGLE,
                                AdDef.ADS_TYPE.INTERSTITIAL
                            )
                            Log.d(TAG, "onAdShowedFullScreenContent: ")
                            rewardedAd = null
                            AdDialog.getInstance().hideLoading()
                            stateLoadAd = StateLoadAd.HAS_BEEN_OPENED

                        }


                        override fun onAdClicked() {
                            super.onAdClicked()
                            callback?.onAdClick()
                        }

                    }
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload &&!isTimeOut) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        AdDialog.getInstance().hideLoading()
                    }, 500)
                    currentActivity?.let { rewardedAd?.show(it,rewardedAdLoadCallback) }
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                loaded = true
                timeLoader = Date().time
                Log.d(TAG, "onAdLoaded: ")
                callbackPreload?.onLoadDone()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                stateLoadAd = StateLoadAd.FAILED
                loadFailed = true
                error = p0.message
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload&&!isTimeOut) {
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad(p0.message)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
                callbackPreload?.onLoadFail()
            }
        }
        RewardedAd.load(activity,id,AdRequest.Builder()
//            .setHttpTimeoutMillis(timeOut.toInt())
            .build(),rewardedAdLoadCallback)

    }
    private val rewardedAdLoadCallback = OnUserEarnedRewardListener {
        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO)
//        Utils.showToastDebug(currentActivity, "Admob Interstitial id: ${adsChild?.adsId}")
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
                    stateLoadAd = StateLoadAd.FAILED
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad("TimeOut")
                    lifecycle?.removeObserver(this)
                } else if (loadFailed || loaded) {
                    AdDialog.getInstance().hideLoading()
                    if (loaded) {
                        currentActivity?.let { rewardedAd?.show(it,rewardedAdLoadCallback) }
                    } else {
                        callback?.onAdFailToLoad(error)
                        stateLoadAd = StateLoadAd.FAILED
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
        isTimeOut = true
        return rewardedAd == null
    }

    override fun destroy() {
        rewardedAd = null
    }

    override fun getStateLoadAd(): StateLoadAd {
        return stateLoadAd
    }

    override fun setPreloadCallback(preloadCallback: PreloadCallback?) {
        callbackPreload = preloadCallback
    }

}
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
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date

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
        RewardedInterstitialAd.load(
            AdRequest.Builder(id).build(),
            object : AdLoadCallback<RewardedInterstitialAd> {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    // Rewarded ad loaded.
                    rewardedAd = ad
                    ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            handle.post {
                                callback?.onAdImpression(AdDef.ADS_TYPE.REWARD_INTERSTITIAL)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            handle.post {
                                callback?.onAdClose(AdDef.ADS_TYPE.REWARD_INTERSTITIAL)
                                rewardedAd = null
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                            handle.post {
                                Log.d(
                                    TAG,
                                    "onAdFailedToShowFullScreenContent: $fullScreenContentError"
                                )
                                rewardedAd = null
                                loadFailed = true
                                error = fullScreenContentError.message
                                callback?.onAdFailToShow(fullScreenContentError.message)
                                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                                    AdDialog.getInstance().hideLoading()
                                    callback?.onAdFailToLoad(fullScreenContentError.message)
                                    lifecycle?.removeObserver(lifecycleObserver)
                                }
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            handle.post {
                                Log.d(TAG, "onAdShowedFullScreenContent: ")
                                rewardedAd = null
                                stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
                                AdDialog.getInstance().hideLoading()
                                Utils.showToastDebug(
                                    activity, "Admob Interstitial id: $idAds"
                                )
                                callback?.onAdShow(
                                    AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_INTERSTITIAL
                                )
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            handle.post {
                                if (AdsController.activity != null && AdsController.activity is AdActivity) {
                                    AdsController.activity?.finish()
                                }
                                callback?.onAdClick()
                            }
                        }

                        override fun onAdPaid(value: AdValue) {
                            super.onAdPaid(value)
                            handle.post {
                                val params = Bundle()
                                params.putString("revenue_micros", value.valueMicros.toString())
                                params.putString("precision_type", value.precisionType.toString())
                                params.putString("ad_unit_id", idAds)
                                callback?.onPaidEvent(params)
                            }
                        }
                    }

                    if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            AdDialog.getInstance().hideLoading()
                        }, 500)
                        currentActivity?.let { rewardedAd?.show(it, rewardedAdLoadCallback) }
                        lifecycle?.removeObserver(lifecycleObserver)
                    }
                    loaded = true
                    timeLoader = Date().time
                    Log.d(TAG, "onAdLoaded: ")
                    stateLoadAd = StateLoadAd.SUCCESS
                    callbackPreload?.onLoadDone()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Rewarded ad failed to load.
                    rewardedAd = null
                    handle.post {
                        stateLoadAd = StateLoadAd.FAILED
                        loadFailed = true
                        error = adError.message
                        if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                            AdDialog.getInstance().hideLoading()
                            callback?.onAdFailToLoad(adError.message)
                            lifecycle?.removeObserver(lifecycleObserver)
                        }
                        callbackPreload?.onLoadFail()
                    }
                }
            },
        )

    }

    private val rewardedAdLoadCallback = OnUserEarnedRewardListener {
        stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_INTERSTITIAL)
        if (AdsController.adActivity != null && AdsController.adActivity is AdActivity) {
//            AdsController.adActivity?.finish()
        }
        //Utils.showToastDebug(currentActivity, "Admob ReWard Interstitial id: ${adsChild?.adsId}")
    }



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
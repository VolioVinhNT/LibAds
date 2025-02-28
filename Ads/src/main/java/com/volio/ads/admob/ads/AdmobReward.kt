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
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.StateADCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date

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
    private var lifecycle: Lifecycle? = null
    private var stateLoadAd = StateLoadAd.NONE
    private var callbackPreload: PreloadCallback? = null

    private fun resetValue() {
        loaded = false
        loadFailed = false
        error = null
    }

    public fun setStateAdCallback(stateADCallback: StateADCallback?) {
        stateADCallback?.onState(stateLoadAd)
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
            stateLoadAd = StateLoadAd.NONE
            rewardedAd?.show(activity, rewardedAdLoadCallback)
            return true
        }
        return false
    }


    private val timeOutCallBack = Runnable {
        if (!loaded && !loadFailed) {
            isTimeOut = true
            if (eventLifecycle == Lifecycle.Event.ON_RESUME) {
                stateLoadAd = StateLoadAd.FAILED
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
        stateLoadAd = StateLoadAd.LOADING
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
        callback = adCallback
        timeClick = System.currentTimeMillis();
        Utils.showToastDebug(
            activity,
            "Admob ReWard id: $idAds"
        )
        val id = if (Constant.isDebug) Constant.ID_ADMOB_REWARD_TEST else idAds
        RewardedAd.load(
            AdRequest.Builder(id).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    // Rewarded ad loaded.
                    rewardedAd = ad
                    ad.adEventCallback = object : RewardedAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            handle.post {
                                callback?.onAdImpression(AdDef.ADS_TYPE.REWARD_VIDEO)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            handle.post {
                                callback?.onAdClose(AdDef.ADS_TYPE.REWARD_VIDEO)
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
                                    AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO
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
        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO)
//        Utils.showToastDebug(currentActivity, "Admob Interstitial id: ${adsChild?.adsId}")
    }


    private val lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            eventLifecycle = event
            if (event == Lifecycle.Event.ON_RESUME) {
                AdDialog.getInstance().hideLoading()
                if (isTimeOut) {
                    stateLoadAd = StateLoadAd.FAILED
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad("TimeOut")
                    lifecycle?.removeObserver(this)
                } else if (loadFailed || loaded) {
                    AdDialog.getInstance().hideLoading()
                    if (loaded) {
                        currentActivity?.let { rewardedAd?.show(it, rewardedAdLoadCallback) }
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
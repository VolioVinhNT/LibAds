package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*

class AdmobReward : AdmobAds() {
    private var error: String? = null

    private var loadFailed = false
    private var loaded: Boolean = false
    private var isTimeOut: Boolean = false
    private var preload: Boolean = false

    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME
    private var rewardedAd: RewardedAd? = null
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private val TAG = "AdmobInterstitial"
    private var activity:Activity? = null
    private var adsChild:AdsChild? = null

    private fun resetValue() {
        loaded = false
        loadFailed = false
        isTimeOut = false
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
        this.activity = activity
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
        this.activity = activity
        this.callback = adCallback
        AdDialog.getInstance().showLoadingWithMessage(activity,loadingText)
        if (loaded && rewardedAd != null) {
            rewardedAd?.show(activity,rewardedAdLoadCallback)
            return true
        }
        return false
    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        textLoading: String?,
        lifecycle: Lifecycle?,
        timeOut: Long,
        adCallback: AdCallback?
    ) {
        this.adsChild = adsChild
        if (System.currentTimeMillis() - timeClick < 500) return
        textLoading?.let {
            AdDialog.getInstance().showLoadingWithMessage(activity, textLoading)
        }
        resetValue()
        callback = adCallback
        timeClick = System.currentTimeMillis();
        val id = if (Constant.isDebug) Constant.ID_ADMOB_REWARD_TEST else adsChild.adsId
        rewardedAd = RewardedAd(activity,id)
        if (!preload) {
            handler.postDelayed(Runnable {
                if (!loaded) {
                    isTimeOut = true
                    error = "TimeOut"
                    if (eventLifecycle == Lifecycle.Event.ON_RESUME) {
                        AdDialog.getInstance().hideLoading()
                        lifecycle?.removeObserver(lifecycleObserver)
                        callback?.onAdFailToLoad(error)
                        Log.d(TAG, "loadAndShow: $error")
                    }
                }
            }, timeOut)
            lifecycle?.addObserver(lifecycleObserver)
        }
       val adListener = object : RewardedAdLoadCallback() {
           override fun onRewardedAdLoaded() {
               super.onRewardedAdLoaded()
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                    AdDialog.getInstance().hideLoading()
                    rewardedAd?.show(activity,rewardedAdLoadCallback)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
               timeLoader = Date().time
               loaded = true
                Log.d(TAG, "onAdLoaded: ")
            }

           override fun onRewardedAdFailedToLoad(p0: LoadAdError?) {
               super.onRewardedAdFailedToLoad(p0)
                loadFailed = true
                error = p0?.message
                if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                    AdDialog.getInstance().hideLoading()
                    callback?.onAdFailToLoad(p0?.message)
                    lifecycle?.removeObserver(lifecycleObserver)
                }
            }

        }
        rewardedAd?.loadAd(AdRequest.Builder().build(),adListener)

    }
    private val rewardedAdLoadCallback = object : RewardedAdCallback() {
        override fun onUserEarnedReward(p0: RewardItem) {
            callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.REWARD_VIDEO)
            Utils.showToastDebug(activity, "Admob Interstitial id: ${adsChild?.adsId}")

        }

        override fun onRewardedAdClosed() {
            super.onRewardedAdClosed()
            callback?.onAdClose(AdDef.ADS_TYPE.REWARD_VIDEO)
            Utils.showToastDebug(activity, "Admob Interstitial id: ${adsChild?.adsId}")


        }

    }

    private val lifecycleObserver = LifecycleEventObserver { source, event ->
        eventLifecycle = event
        if (event == Lifecycle.Event.ON_RESUME) {
            AdDialog.getInstance().hideLoading()
            if (isTimeOut || loadFailed || loaded) {
                AdDialog.getInstance().hideLoading()
                if (loaded) {
                    rewardedAd?.show(activity,rewardedAdLoadCallback)
                } else {
                    callback?.onAdFailToLoad(error)
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
        rewardedAd = null
    }

}
package com.volio.ads.fan.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener

import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*

class FanInterstitial : FanAds() {
    private var error: String? = null

    private var loadFailed = false
    private var loaded: Boolean = false
    private var isTimeOut: Boolean = false
    private var preload: Boolean = false

    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME
    private var mInterstitialAd: InterstitialAd? = null
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private val TAG = "FanInterstitial"

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
        callback = adCallback
        AdDialog.getInstance().showLoadingWithMessage(activity,loadingText)
        if (loaded && mInterstitialAd != null) {
            mInterstitialAd?.show()
            return true
        }
//        else {
//            adCallback?.onAdFailToLoad(error)
//        }
        return false;
    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        textLoading: String?,
        lifecycle: Lifecycle?,
        timeOut: Long,
        adCallback: AdCallback?
    ) {
        if (System.currentTimeMillis() - timeClick < 500) return
        textLoading?.let {
            AdDialog.getInstance().showLoadingWithMessage(activity, textLoading)
        }
        Utils.showToastDebug(activity, "Fan Interstitial id: ${adsChild.adsId}")
        Log.d(TAG, "load: "+textLoading)
        resetValue()
        callback = adCallback
        timeClick = System.currentTimeMillis();
        val id =  adsChild.adsId
        mInterstitialAd = InterstitialAd(activity,id)
        if (!preload) {
            handler.postDelayed(Runnable {
                if (!loaded&&!loadFailed) {
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
        mInterstitialAd?.let {
            it.loadAd(it.buildLoadAdConfig()
                .withAdListener(object : InterstitialAdListener{
                    override fun onInterstitialDisplayed(p0: Ad?) {
                        AdDialog.getInstance().hideLoading()
                        callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.INTERSTITIAL)
                        Log.d(TAG, "onInterstitialDisplayed: ")
                    }

                    override fun onAdClicked(p0: Ad?) {
                        adCallback?.onAdClick()
                    }

                    override fun onInterstitialDismissed(p0: Ad?) {
                        callback?.onAdClose(AdDef.ADS_TYPE.INTERSTITIAL)
                        Log.d(TAG, "onAdClosed: ")
                    }

                    override fun onError(p0: Ad?, p1: AdError?) {
                        loadFailed = true
                        error = p1?.errorMessage
                        if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                            AdDialog.getInstance().hideLoading()
                            callback?.onAdFailToLoad(p1?.errorMessage)
                            lifecycle?.removeObserver(lifecycleObserver)
                        }
                        Log.d(TAG, "onError: $error")
                    }

                    override fun onAdLoaded(p0: Ad?) {
                        if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload && !isTimeOut) {
                            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                                AdDialog.getInstance().hideLoading()
                            },500)
                            mInterstitialAd?.show()
                            lifecycle?.removeObserver(lifecycleObserver)

                        }
                        loaded = true
                        timeLoader = Date().time
                        Log.d(TAG, "onAdLoaded: ")


                    }

                    override fun onLoggingImpression(p0: Ad?) {
                        Log.d(TAG, "onLoggingImpression: ")
                    }

                })
                .build())
        }
//        mInterstitialAd?.loadAd( )

    }

    private val lifecycleObserver = LifecycleEventObserver { source, event ->
        eventLifecycle = event
        if (event == Lifecycle.Event.ON_RESUME) {
//            AdDialog.getInstance().hideLoading()
            if (isTimeOut || loadFailed || loaded) {
                AdDialog.getInstance().hideLoading()
                if (loaded) {
                    mInterstitialAd?.show()
                    Log.d(TAG, "show: ")
                } else {
                    callback?.onAdFailToLoad(error)
                    Log.d(TAG, "faild: ")
                }
            }
        }

    }

    override fun isLoaded(): Boolean {
        return loaded
    }

    override fun isDestroy(): Boolean {
        return mInterstitialAd == null
    }

    override fun destroy() {
        mInterstitialAd = null
    }

}
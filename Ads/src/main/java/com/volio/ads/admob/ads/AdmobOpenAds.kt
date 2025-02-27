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
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.AdDialog
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date

class AdmobOpenAds : AdmobAds() {
    private var timeClick = 0L
    private var callback: AdCallback? = null
    private var error: String? = null
    private var isTimeOut = false
    private var handler = Handler(Looper.getMainLooper())
    private var loadFailed = false
    private var loaded: Boolean = false
    private var preload: Boolean = false


    private var eventLifecycle: Lifecycle.Event = Lifecycle.Event.ON_RESUME

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var lifecycle: Lifecycle? = null

    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE
    val TAG = "AdmobOpenAds"
    override fun loadAndShow(
        activity: Activity,
        idAds: String,
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
            idAds,
            lifecycle,
            timeMillisecond ?: Constant.TIME_OUT_DEFAULT,
            adCallback
        )
    }

    override fun preload(activity: Activity, idAds: String) {
        preload = true
        load(activity, idAds)
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
            if (eventLifecycle == Lifecycle.Event.ON_RESUME) {
                callback?.onAdFailToLoad("TimeOut")
                lifecycle?.removeObserver(lifecycleObserver)
            }
        }
    }

    private fun load(
        activity: Activity,
        idAds: String,
        lifecycle: Lifecycle? = null,
        timeOut: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? = null
    ) {
        this.lifecycle = lifecycle
        this.callback = adCallback
        stateLoadAd = StateLoadAd.LOADING
        timeClick = System.currentTimeMillis();
        val id = if (Constant.isDebug) Constant.ID_ADMOB_OPEN_APP_TEST else idAds
        if (!preload) {
            lifecycle?.addObserver(lifecycleObserver)
            handler.removeCallbacks(timeOutCallBack)
            handler.postDelayed(timeOutCallBack, timeOut)
        }
        resetValue()

        AppOpenAd.load(
            AdRequest.Builder(id).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad


                    ad.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            handle.post {
                                callback?.onAdImpression(AdDef.ADS_TYPE.OPEN_APP)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            handle.post {
                                callback?.onAdClose(AdDef.ADS_TYPE.OPEN_APP)
                                appOpenAd = null
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                            handle.post {
                                Log.d(TAG, "onAdFailedToShowFullScreenContent: $fullScreenContentError")
                                appOpenAd = null
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
                                appOpenAd = null
                                stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
                                AdDialog.getInstance().hideLoading()
                                Utils.showToastDebug(
                                    activity, "Admob Interstitial id: $idAds"
                                )
                                callback?.onAdShow(
                                    AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.OPEN_APP
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

                    handle.post {
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
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    handle.post {
                        loadFailed = true
                        error = loadAdError.message
                        if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                            AdDialog.getInstance().hideLoading()
                            lifecycle?.removeObserver(lifecycleObserver)
                            if (!isTimeOut) {
                                callback?.onAdFailToLoad(loadAdError.message)
                            }
                        }
                        stateLoadAd = StateLoadAd.FAILED
                        callbackPreload?.onLoadFail()
                        Utils.showToastDebug(
                            activity,
                            "Admob OpenAds Fail id: ${idAds}"
                        )
                    }
                }
            },
        )

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
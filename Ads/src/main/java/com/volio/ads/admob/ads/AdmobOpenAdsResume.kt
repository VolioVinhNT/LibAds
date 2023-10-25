package com.volio.ads.admob.ads

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.volio.ads.AdCallback
import com.volio.ads.AdsController
import com.volio.ads.PreloadCallback
import com.volio.ads.R
import com.volio.ads.utils.*
import java.util.*

class AdmobOpenAdsResume : AdmobAds() {
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
    private var dialog: Dialog? = null
    val TAG = "AdmobOpenAdsResume"
    private fun showDialogLoading(activity: Activity) {
        dialog?.dismiss()
        dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog?.setCancelable(false)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.dialog_text_loading)
        dialog?.show()
    }

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
            showDialogLoading(activity)
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
                dialog?.dismiss()
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
            showDialogLoading(activity)
            lifecycle?.addObserver(lifecycleObserver)
            handler.removeCallbacks(timeOutCallBack)
            handler.postDelayed(timeOutCallBack, timeOut)
        }
        Log.d(TAG, "load: ")
        resetValue()
        val openAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(p0: AppOpenAd) {
                appOpenAd = p0
                appOpenAd?.onPaidEventListener = OnPaidEventListener {
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
                appOpenAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {

                        override fun onAdImpression() {
                            super.onAdImpression()
                            adCallback?.onAdImpression(AdDef.ADS_TYPE.OPEN_APP)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            Log.d(TAG, "onAdDismissedFullScreenContent: ")
                            callback?.onAdClose(AdDef.ADS_TYPE.OPEN_APP)
                            appOpenAd = null
                            dialog?.dismiss()

                            //// perform your code that you wants to do after ad dismissed or closed
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            super.onAdFailedToShowFullScreenContent(adError)
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: ")
                            appOpenAd = null
                            loadFailed = true
                            error = adError.message
                            if (eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
                                callback?.onAdFailToLoad(adError.message)
                                lifecycle?.removeObserver(lifecycleObserver)
                                dialog?.dismiss()
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            callback?.onAdClick()
                            if (AdsController.adActivity != null && AdsController.adActivity is AdActivity) {
                                AdsController.adActivity?.finish()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            Log.d(TAG, "onAdShowedFullScreenContent: ")
                            appOpenAd = null
                            Utils.showToastDebug(
                                activity,
                                "Admob OpenAds id: $idAds"
                            )
                            stateLoadAd = StateLoadAd.HAS_BEEN_OPENED
                            callback?.onAdShow(
                                AdDef.NETWORK.GOOGLE,
                                AdDef.ADS_TYPE.OPEN_APP
                            )
                        }
                    }
                if (!isTimeOut && eventLifecycle == Lifecycle.Event.ON_RESUME && !preload) {
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
                    lifecycle?.removeObserver(lifecycleObserver)
                    if (!isTimeOut) {
                        callback?.onAdFailToLoad(p0.message)
                        dialog?.dismiss()
                    }
                }
                stateLoadAd = StateLoadAd.FAILED
                callbackPreload?.onLoadFail()
            }
        }
        val request: AdRequest = AdRequest.Builder()
//            .setHttpTimeoutMillis(timeOut.toInt())
            .build()
        AppOpenAd.load(activity, id, request, openAdLoadCallback)

    }

    private val lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            eventLifecycle = event
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isTimeOut) {
                    callback?.onAdFailToLoad("TimeOut")
                    lifecycle?.removeObserver(this)
                    dialog?.dismiss()
                } else if (loadFailed || loaded) {
                    if (loaded) {
                        currentActivity?.let { appOpenAd?.show(it) }
                    } else {
                        callback?.onAdFailToLoad(error)
                        dialog?.dismiss()
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
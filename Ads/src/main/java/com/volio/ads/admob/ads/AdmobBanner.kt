package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.*

class AdmobBanner : AdmobAds() {
    private var adView: AdView? = null
    private var callback: AdCallback? = null
    private var stateLoadAd: StateLoadAd = StateLoadAd.NONE
    private var callbackPreload: PreloadCallback? = null
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

        group = layout
        callback = adCallback

        load(activity, group, adsChild, callback, loadSuccess = {
            show(activity, adsChild, loadingText, group, layoutAds, lifecycle, callback)
        })
    }

    private var group: ViewGroup? = null
    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean {
        group = layout
        callback = adCallback
        if (adView != null && layout != null) {
            layout.removeAllViews()
            (adView?.parent as ViewGroup?)?.removeAllViews()
            layout.addView(adView)
            callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.BANNER)
            return true
        } else {
            Utils.showToastDebug(activity, "layout ad native not null")
        }
        return false
    }

    override fun setPreloadCallback(preloadCallback: PreloadCallback?) {
        callbackPreload = preloadCallback
    }

    public override fun preload(activity: Activity, adsChild: AdsChild) {
        load(activity, null, adsChild, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        layout: ViewGroup?,
        adsChild: AdsChild,
        adCallback: AdCallback?,
        loadSuccess: () -> Unit
    ) {
//        adsChild.adsSize = AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250
        adsChild.adsSize = adsChild.adsSize
        isLoadSuccess = false
        stateLoadAd = StateLoadAd.LOADING
        callback = adCallback
        val id: String = if (Constant.isDebug) {
            Constant.ID_ADMOB_BANNER_TEST
        } else {
            adsChild.adsId
        }
        adView = AdView(activity)
        val adSize = getAdsize(adsChild.adsSize)

        layout?.let { viewG ->
            val lp = viewG.layoutParams
            lp.width = adSize?.getWidthInPixels(viewG.context) ?: 0
            lp.height = adSize?.getHeightInPixels(viewG.context) ?: 0
            viewG.layoutParams = lp
        }


        adSize?.let {
            adView?.setAdSize(it)
        }
        adView?.adUnitId = id
        adView?.loadAd(AdRequest.Builder().build())
        adView?.setOnPaidEventListener {
            kotlin.runCatching {
                val params = Bundle()
                params.putString("revenue_micros", it.valueMicros.toString())
                params.putString("currency", it.currencyCode)
                params.putString("precision_type", it.precisionType.toString())
                params.putString("ad_unit_id", adView?.adUnitId)
                val adapterResponseInfo = adView?.responseInfo?.loadedAdapterResponseInfo
                adapterResponseInfo?.let { it ->
                    params.putString("ad_source_id", it.adSourceId)
                    params.putString("ad_source_name", it.adSourceName)
                    params.putString("ad_source_instance_id", it.adSourceInstanceId)
                    params.putString("ad_source_instance_name", it.adSourceInstanceName)
                    params.putString("latency", it.latencyMillis.toString())
                }
                callback?.onPaidEvent(params)
            }
        }
        adView?.adListener = object : AdListener() {
            override fun onAdImpression() {
                super.onAdImpression()
                Log.e("TAG", "onAdImpression: " )
                Firebase.analytics.logEvent(Constant.KeyCustomImpression, Bundle.EMPTY)
            }

            override fun onAdOpened() {
                super.onAdOpened()
                Utils.showToastDebug(activity, "Admob Banner id: ${adsChild.adsId}")
                callback?.onAdClick()

            }

            override fun onAdClosed() {
                super.onAdClosed()
                callback?.onAdClose(AdDef.ADS_TYPE.BANNER)
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                stateLoadAd = StateLoadAd.FAILED
                callbackPreload?.onLoadFail()

            }

            override fun onAdLoaded() {
                super.onAdLoaded()
//                group?.removeAllViews()
                stateLoadAd = StateLoadAd.SUCCESS
                isLoadSuccess = true
                timeLoader = Date().time
                callbackPreload?.onLoadDone()
                loadSuccess()
            }
        }
    }

    private var isLoadSuccess = false

    private fun getAdsize(adsize: String): AdSize? {
        if (adsize == AdDef.GOOGLE_AD_BANNER.BANNER_320x50) {
            return AdSize.BANNER
        }
        if (adsize == AdDef.GOOGLE_AD_BANNER.FULL_BANNER_468x60) {
            return AdSize.FULL_BANNER
        }
        if (adsize == AdDef.GOOGLE_AD_BANNER.LARGE_BANNER_320x100) {
            return AdSize.LARGE_BANNER
        }
        if (adsize == AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250) {
            return AdSize.MEDIUM_RECTANGLE
        }
        if (adsize == AdDef.GOOGLE_AD_BANNER.SMART_BANNER) {
            return AdSize.SMART_BANNER
        }
        return if (adsize == AdDef.GOOGLE_AD_BANNER.LEADERBOARD_728x90) {
            AdSize.LEADERBOARD
        } else AdSize.BANNER
    }

    override fun destroy() {
        adView = null
        isLoadSuccess = false
    }

    override fun isDestroy(): Boolean {
        return adView == null
    }

    override fun isLoaded(): Boolean {
        return isLoadSuccess
    }

    override fun getStateLoadAd(): StateLoadAd {
        return stateLoadAd
    }
}
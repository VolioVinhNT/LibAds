package com.volio.ads.fan.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.facebook.ads.*
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Utils

class FanBanner : FanAds(){
    private val TAG = "FanBanner"
    private var bannerAdView: AdView? = null
    private var isLoader:Boolean = false

    override fun isDestroy(): Boolean {
        return bannerAdView == null
    }

    override fun isLoaded(): Boolean {
        return isLoader
    }

    override fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
    }

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
        load(activity,adsChild,adCallback,loadSuccess = {
            show(activity,adsChild,loadingText,layout,layoutAds,adCallback)
        })

    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        adCallback: AdCallback?, loadSuccess: () -> Unit
    ) {
        Log.d(TAG, "load: ")
        isLoader = false
        bannerAdView?.destroy()
        bannerAdView = null
        bannerAdView = AdView(activity, adsChild.adsId, AdSize.BANNER_HEIGHT_50)
        bannerAdView?.let { nonNullBannerAdView ->
            nonNullBannerAdView.loadAd(
                nonNullBannerAdView.buildLoadAdConfig().withAdListener(object : AdListener{
                    override fun onAdClicked(p0: Ad?) {
                        adCallback?.onAdClick()
                        Log.d(TAG, "onAdClicked: ")
                        Utils.showToastDebug(activity,"id native: ${adsChild.adsId}")

                    }

                    override fun onError(p0: Ad?, p1: AdError?) {
                        Log.d(TAG, "onError: ")

                    }

                    override fun onAdLoaded(p0: Ad?) {
                        loadSuccess()
                        isLoader = true
                        Log.d(TAG, "onAdLoaded: ")
                    }

                    override fun onLoggingImpression(p0: Ad?) {
                        Log.d(TAG, "onLoggingImpression: ")
                    }

                }).build())
        }
    }
    override fun preload(activity: Activity, adsChild: AdsChild) {
        load(activity,adsChild,null,loadSuccess = {

        })
    }

    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        adCallback: AdCallback?
    ): Boolean {
        if (bannerAdView != null && layout != null) {
            layout.removeAllViews()
            layout.addView(bannerAdView)
            adCallback?.onAdShow(AdDef.NETWORK.FACEBOOK, AdDef.ADS_TYPE.BANNER)
            return true
        } else {
            Utils.showToastDebug(activity, "layout ad native not null")
        }
        return false
    }

}
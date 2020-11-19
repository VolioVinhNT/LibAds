package com.volio.ads.admob.ads

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*

class AdmobBanner : AdmobAds() {
    private var adView: AdView? = null
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
        layout?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                layout.measuredHeight.let {
                    if(Utils.convertPixelsToDp(it.toFloat(),activity) >= 250f ){
                        adsChild.adsSize = AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250
                    }
                }
                load(activity, adsChild, adCallback, loadSuccess = {
                    show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
                })
            }

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
        if (adView != null && layout != null) {
            layout.removeAllViews()
            layout.addView(adView)
            adCallback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.BANNER)
            return true
        } else {
            Utils.showToastDebug(activity, "layout ad native not null")
        }
        return false
    }

    public override fun preload(activity: Activity, adsChild: AdsChild) {
        load(activity, adsChild, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        adCallback: AdCallback?, loadSuccess: () -> Unit
    ) {
        isLoadSuccess = false
        val id: String = if (Constant.isDebug) {
            Constant.ID_ADMOB_BANNER_TEST
        } else {
            adsChild.adsId
        }
        adView = AdView(activity)
        adView?.adSize = getAdsize(adsChild.adsSize)
        adView?.setBackgroundColor(Color.WHITE)
        adView?.adUnitId = id
        adView?.loadAd(AdRequest.Builder().build())
        adView?.adListener = object : AdListener() {
            override fun onAdOpened() {
                super.onAdOpened()
                Utils.showToastDebug(activity,"Admob Banner id: ${adsChild.adsId}")

            }

            override fun onAdClosed() {
                super.onAdClosed()
                adCallback?.onAdClose(AdDef.ADS_TYPE.BANNER)
            }

            override fun onAdFailedToLoad(p0: LoadAdError?) {
                super.onAdFailedToLoad(p0)

            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                isLoadSuccess = true
                timeLoader = Date().time
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
        return !isLoadSuccess
    }

    override fun isLoaded(): Boolean {
        return isLoadSuccess
    }
}
package com.volio.ads.admob.ads

import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.volio.ads.AdCallback
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*


class AdmobAdaptiveBanner : AdmobAds() {
    private var isLoadSuccess = false
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
        load(activity, adsChild, adCallback, loadSuccess = {
            show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
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
    override fun preload(activity: Activity,adsChild: AdsChild){
        load(activity,adsChild,null,loadSuccess = {

        })
    }
    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        adCallback: AdCallback?, loadSuccess: () -> Unit
    ) {
        val id: String = if (Constant.isDebug) {
            Constant.ID_ADMOB_BANNER_TEST
        } else {
            adsChild.adsId
        }
        isLoadSuccess = false
        adView = AdView(activity.applicationContext)
        adView?.setBackgroundColor(Color.WHITE)
        adView?.adUnitId = id
        adView?.adSize = getAdsize(activity)

//        adView?.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
//            activity,
//            getScreenWidth(activity)
//        )
        adView?.loadAd(
            AdRequest.Builder().build()
        )
        adView?.adListener = object : AdListener() {
            override fun onAdClicked() {
                super.onAdClicked()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                Utils.showToastDebug(activity, "Admob AdaptiveBanner id: ${adsChild.adsId}")
                adCallback?.onAdClick()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                adCallback?.onAdClose(AdDef.NETWORK.GOOGLE)
            }

            override fun onAdFailedToLoad(p0: LoadAdError?) {
                super.onAdFailedToLoad(p0)
                Utils.showToastDebug(activity, "Admob AdaptiveBanner id: ${p0?.message}")
                adCallback?.onAdFailToLoad(p0?.message)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                isLoadSuccess = true
                loadSuccess()
                timeLoader = Date().time
            }
        }

    }

    private fun getAdsize(activity: Activity): AdSize? {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            activity,
            adWidth
        )
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

}
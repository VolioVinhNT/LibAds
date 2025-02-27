package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.*
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.*

class AdmobBanner : AdmobAds() {
    private var adView: BannerAd? = null
    private var callback: AdCallback? = null
    private var stateLoadAd: StateLoadAd = StateLoadAd.NONE
    private var callbackPreload: PreloadCallback? = null
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

        group = layout
        callback = adCallback

        load(activity, group, idAds, callback, loadSuccess = {
            show(activity, idAds, loadingText, group, layoutAds, lifecycle, callback)
        })
    }

    private var group: ViewGroup? = null
    override fun show(
        activity: Activity,
        idAds: String,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        adCallback: AdCallback?
    ): Boolean {
        callback = adCallback
        if (adView != null && layout != null) {
            try {
                callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.BANNER)
                layout.removeAllViews()
                val view = adView?.getView(activity)
                if (view != null) {
                    (view.parent as ViewGroup).removeView(view) // <- fix
                }
                layout.addView(view)
            } catch (_: Exception) {
            }

            return true
        } else {
            Utils.showToastDebug(activity, "layout ad native not null")
        }
        return false
    }

    override fun setPreloadCallback(preloadCallback: PreloadCallback?) {
        callbackPreload = preloadCallback
    }

    public override fun preload(activity: Activity, idAds: String) {
        load(activity, null, idAds, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        layout: ViewGroup?,
        idAds: String,
        adCallback: AdCallback?,
        loadSuccess: () -> Unit
    ) {
        callback = adCallback
        val id: String = if (Constant.isDebug) {
            Constant.ID_ADMOB_BANNER_TEST
        } else {
            idAds
        }
        stateLoadAd = StateLoadAd.LOADING
        isLoadSuccess = false
        val adSize = getAdsSize(AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250)
        val adRequest = BannerAdRequest.Builder(id, adSize).build()
        adSize.let {
            Log.e("TAGEG", "load: ${it.width}")
            Log.e("TAGEG", "load: ${it.height}")
            Log.e("TAGEG", "layout?.width : ${layout?.width ?: 1}")
        }
        BannerAd.load(
            adRequest,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    // Assign the loaded ad to the BannerAd object.
                    adView = ad
                    adView?.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            handle.post {
                                callback?.onAdImpression(AdDef.ADS_TYPE.BANNER)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            handle.post {
                                callback?.onAdClose(AdDef.NETWORK.GOOGLE)
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            handle.post {
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
                    adView?.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            super.onAdRefreshed()
                            handle.post {
                                callback?.onAdRefreshed()
                            }
                        }

                        override fun onAdFailedToRefresh(adError: LoadAdError) {
                            super.onAdFailedToRefresh(adError)
                            handle.post {
                                callback?.onAdFailedToRefresh(adError.message)
                            }
                        }
                    }
                    handle.post {
                        stateLoadAd = StateLoadAd.SUCCESS
                        isLoadSuccess = true
                        callbackPreload?.onLoadDone()
                        loadSuccess()
                        timeLoader = Date().time
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    handle.post {
                        Utils.showToastDebug(activity, "Admob AdapBanner: ${adError.message}")
                        callback?.onAdFailToLoad(adError.message)
                        stateLoadAd = StateLoadAd.FAILED
                        callbackPreload?.onLoadFail()
                    }
                }

            },
        )

        layout?.post {
            layout.let { viewG ->
                val lp = viewG.layoutParams
                lp.width = adSize.getWidthInPixels(viewG.context)
                lp.height = adSize.getHeightInPixels(viewG.context)
                viewG.layoutParams = lp
            }

        }
    }

    private var isLoadSuccess = false

    private fun getAdsSize(adsSize: String): AdSize {
        if (adsSize == AdDef.GOOGLE_AD_BANNER.BANNER_320x50) {
            return AdSize.BANNER
        }
        if (adsSize == AdDef.GOOGLE_AD_BANNER.FULL_BANNER_468x60) {
            return AdSize.FULL_BANNER
        }
        if (adsSize == AdDef.GOOGLE_AD_BANNER.LARGE_BANNER_320x100) {
            return AdSize.LARGE_BANNER
        }
        if (adsSize == AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250) {
            return AdSize.MEDIUM_RECTANGLE
        }
        return if (adsSize == AdDef.GOOGLE_AD_BANNER.LEADERBOARD_728x90) {
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
package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date


class AdmobAdaptiveBanner : AdmobAds() {
    private var isLoadSuccess = false
    private var adView: BannerAd? = null
    private var callback: AdCallback? = null
    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE
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
        callback = adCallback
        load(activity, idAds, layout, callback, loadSuccess = {
            show(activity, idAds, loadingText, layout, layoutAds, lifecycle, callback)
        })
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
        callback = adCallback
        if (adView != null && layout != null) {
            try {
                callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.BANNER_ADAPTIVE)
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

    override fun preload(activity: Activity, idAds: String) {
        load(activity, idAds, null, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        idAds: String,
        layout: ViewGroup?,
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
        val adSize = getAdsize(activity)
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
                                callback?.onAdImpression(AdDef.ADS_TYPE.BANNER_ADAPTIVE)
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

    private fun getAdsize(activity: Activity): AdSize {
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
        adView?.destroy()
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
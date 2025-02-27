package com.volio.ads.admob.ads


import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.R
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date

class AdmobNative : AdmobAds() {
    var currentUnifiedNativeAd: NativeAd? = null
    private var activity: Activity? = null
    private var adCallbackMain: AdCallback? = null
    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE
    private val TAG = "AdmobNative"
    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        var mediaView: MediaView? = null
        if (viewGroup != null) {
            mediaView = MediaView(adView.context)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        val viewGroupIcon = adView.findViewById<View>(R.id.ad_app_icon)
        if (viewGroupIcon != null) {
            if (viewGroupIcon is ViewGroup) {
                val nativeAdIcon = ImageView(adView.context)
                viewGroupIcon.addView(
                    nativeAdIcon,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                adView.iconView = nativeAdIcon
            } else {
                adView.iconView = viewGroupIcon
            }

        }

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        if (adView.headlineView is TextView){
            (adView.headlineView as TextView).text = nativeAd.headline
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            if (adView.bodyView is TextView) {
                adView.bodyView?.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }
        }
        if (adView.callToActionView != null) {
            if (adView.callToActionView != null) {
                if (nativeAd.callToAction == null) {
                    adView.callToActionView?.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = View.VISIBLE
                    if (adView.callToActionView is Button) {
                        (adView.callToActionView as Button).text = nativeAd.callToAction
                    } else {
                        (adView.callToActionView as TextView).text = nativeAd.callToAction
                    }
                }
            }
        }
        if (adView.iconView != null) {
            if (nativeAd.icon == null) {
                adView.iconView?.visibility = View.INVISIBLE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon?.drawable
                )
                adView.iconView?.visibility = View.VISIBLE
            }
        }
        if (adView.priceView != null) {
            if (nativeAd.price == null) {
                adView.priceView?.visibility = View.INVISIBLE
            } else {
                adView.priceView?.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }
        }
        if (adView.storeView != null) {
            if (nativeAd.store == null) {
                adView.storeView?.visibility = View.INVISIBLE
            } else {
                adView.storeView?.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }
        }
        if (adView.starRatingView != null) {
            if (nativeAd.starRating == null) {
                adView.starRatingView?.visibility = View.INVISIBLE
            } else {
                if (adView.starRatingView is RatingBar){
                    nativeAd.starRating?.toFloat()?.let {
                        (adView.starRatingView as RatingBar).rating = it
                    }
                    adView.starRatingView?.visibility = View.VISIBLE
                }
            }
        }
        if (adView.advertiserView != null) {
            if (nativeAd.advertiser == null) {
                adView.advertiserView?.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView).text = nativeAd.advertiser
                adView.advertiserView?.visibility = View.VISIBLE
            }
        }
        adView.registerNativeAd(nativeAd, mediaView)

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
        Log.d(TAG, "loadAndShow: ")
        load(activity, idAds, adCallback, loadSuccess = {
            show(activity, idAds, loadingText, layout, layoutAds, lifecycle, adCallback)
        })
    }

    override fun preload(activity: Activity, idAds: String) {
        load(activity, idAds, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        idAds: String,
        adCallback: AdCallback?,
        loadSuccess: () -> Unit
    ) {
        adCallbackMain = adCallback
        stateLoadAd = StateLoadAd.LOADING
        this.activity = activity
        val id = if (Constant.isDebug) Constant.ID_ADMOB_NATIVE_TEST else idAds

        val adRequest = NativeAdRequest
            .Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
            .build()

        val adCallback =
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    stateLoadAd = StateLoadAd.SUCCESS
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            handle.post {
                                adCallbackMain?.onAdImpression(AdDef.ADS_TYPE.NATIVE)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            handle.post {
                                adCallbackMain?.onAdClose(AdDef.NETWORK.GOOGLE)
                            }
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            handle.post {
                                adCallbackMain?.onAdClick()
                            }
                        }

                        override fun onAdPaid(value: AdValue) {
                            super.onAdPaid(value)
                            handle.post {
                                val params = Bundle()
                                params.putString("revenue_micros", value.valueMicros.toString())
                                params.putString("precision_type", value.precisionType.toString())
                                params.putString("ad_unit_id", idAds)
                                adCallbackMain?.onPaidEvent(params)
                            }
                        }
                    }

                    handle.post {
                        callbackPreload?.onLoadDone()
                        if (currentUnifiedNativeAd != null) {
                            currentUnifiedNativeAd?.destroy()
                        }
                        currentUnifiedNativeAd?.destroy()
                        currentUnifiedNativeAd = nativeAd
                        loadSuccess()
                    }
                    timeLoader = Date().time
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    handle.post {
                        Log.d(TAG, "onAdFailedToLoad: ${adError.code} " + adError.message)
                        adCallbackMain?.onAdFailToLoad(adError.message)
                        stateLoadAd = StateLoadAd.FAILED
                        callbackPreload?.onLoadFail()
                    }
                }
            }
        NativeAdLoader.load(adRequest,adCallback)

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
        Log.d(TAG, "show: ${layout == null}")
        adCallbackMain = adCallback
        adCallbackMain?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.NATIVE)

        if (layout != null) {
            if (layoutAds != null) {


                val unifiedNativeAdView = NativeAdView(activity)
                unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutAds.parent?.let {
                    (it as ViewGroup).removeView(layoutAds)
                }
                kotlin.runCatching {
                    layoutAds.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                unifiedNativeAdView.addView(layoutAds)
                currentUnifiedNativeAd?.let {
                    layout.removeAllViews()
                    layout.addView(unifiedNativeAdView)
                    populateUnifiedNativeAdView(it, unifiedNativeAdView)
                }
            } else {
                val adView = LayoutInflater.from(activity)
                    .inflate(R.layout.ad_unified, null) as NativeAdView
                currentUnifiedNativeAd?.let {
                    populateUnifiedNativeAdView(it, adView)
                    layout.removeAllViews()
                    layout.addView(adView)
                }

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

    public override fun isDestroy(): Boolean = (currentUnifiedNativeAd == null)
    public override fun destroy() {
        currentUnifiedNativeAd?.destroy()
        currentUnifiedNativeAd = null;
    }

    public override fun isLoaded(): Boolean {
        return currentUnifiedNativeAd != null
    }

    override fun getStateLoadAd(): StateLoadAd {
        return stateLoadAd
    }

}
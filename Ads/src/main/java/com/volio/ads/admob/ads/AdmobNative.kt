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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd

import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.R
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.*

class AdmobNative : AdmobAds() {
    var currentUnifiedNativeAd: NativeAd? = null
    private var activity: Activity? = null
    private var adsChild: AdsChild? = null
    private var adCallbackMain: AdCallback? = null
    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE

    private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        if (viewGroup != null) {
            val mediaView = MediaView(adView.context)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            adView.mediaView = mediaView
        } else {
            if (Constant.isDebug) {
                val mediaView = MediaView(adView.context)
                adView.addView(
                    mediaView,
                    ViewGroup.LayoutParams(
                        0,
                        0
                    )
                )
                adView.mediaView = mediaView
            }
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
        (adView.headlineView as TextView).text = nativeAd.headline
        if (adView.mediaView != null) {
            adView.mediaView.setMediaContent(nativeAd.mediaContent)
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView.visibility = View.INVISIBLE
        } else {
            adView.bodyView.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (adView.callToActionView != null) {
            if (adView.callToActionView != null) {
                if (nativeAd.callToAction == null) {
                    adView.callToActionView.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView.visibility = View.VISIBLE
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
                adView.iconView.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon.drawable
                )
                adView.iconView.visibility = View.VISIBLE
            }
        }
        if (adView.priceView != null) {
            if (nativeAd.price == null) {
                adView.priceView.visibility = View.INVISIBLE
            } else {
                adView.priceView.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }
        }
        if (adView.storeView != null) {
            if (nativeAd.store == null) {
                adView.storeView.visibility = View.INVISIBLE
            } else {
                adView.storeView.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }
        }
        if (adView.starRatingView != null) {
            if (nativeAd.starRating == null) {
                adView.starRatingView.visibility = View.INVISIBLE
            } else {
                (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView.visibility = View.VISIBLE
            }
        }
        if (adView.advertiserView != null) {
            if (nativeAd.advertiser == null) {
                adView.advertiserView.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView).text = nativeAd.advertiser
                adView.advertiserView.visibility = View.VISIBLE
            }
        }

        adView.setNativeAd(nativeAd)
//        val vc = nativeAd.videoController
//
//        if (vc.hasVideoContent()) {
//            vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {
//                override fun onVideoEnd() {
//                    super.onVideoEnd()
//                }
//            }
//        } else {
//        }

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
        load(activity, adsChild, adCallback, loadSuccess = {
            show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
        })
    }

    override fun preload(activity: Activity, adsChild: AdsChild) {
        load(activity, adsChild, null, loadSuccess = {

        })
    }

    private fun load(
        activity: Activity,
        adsChild: AdsChild,
        adCallback: AdCallback?,
        loadSuccess: () -> Unit
    ) {
        adCallbackMain = adCallback
        stateLoadAd = StateLoadAd.LOADING
        this.adsChild = adsChild
        this.activity = activity
        val idAds = if (Constant.isDebug) Constant.ID_ADMOB_NATIVE_TEST else adsChild.adsId
        val builder = AdLoader.Builder(activity.applicationContext, idAds)
        builder.forNativeAd { unifiedNativeAd ->
            Log.d(TAG, "load: ")
            adCallbackMain?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.NATIVE)
            unifiedNativeAd?.setOnPaidEventListener {
                kotlin.runCatching {
                    val params = Bundle()
                    params.putString("valuemicros", it.valueMicros.toString())
                    params.putString("currency", it.currencyCode)
                    params.putString("precision", it.precisionType.toString())
                    params.putString("adunitid", idAds)
                    params.putString(
                        "network",
                        unifiedNativeAd.responseInfo?.mediationAdapterClassName
                    )
                    adCallbackMain?.onPaidEvent(params)
                }
            }
            if (currentUnifiedNativeAd != null) {
                currentUnifiedNativeAd?.destroy()
            }

            currentUnifiedNativeAd?.destroy()
            currentUnifiedNativeAd = unifiedNativeAd
            loadSuccess()
            timeLoader = Date().time

        }

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()
        builder.withNativeAdOptions(adOptions)
        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad: ${loadAdError.code} " + loadAdError.message)
                adCallbackMain?.onAdFailToLoad(loadAdError.message)
                stateLoadAd = StateLoadAd.FAILED
                callbackPreload?.onLoadFail()
            }

            override fun onAdLoaded() {
                stateLoadAd = StateLoadAd.SUCCESS
                callbackPreload?.onLoadDone()
                super.onAdLoaded()
            }

            override fun onAdClicked() {
//                Log.i("dsadsadsadsadsdsa","ad click")
//                super.onAdClicked()
//                Utils.showToastDebug(activity,"id native: ${adsChild.adsId}")
//                adCallbackMain?.onAdClick()

            }

            override fun onAdOpened() {
                Utils.showToastDebug(activity, "id native: ${adsChild.adsId}")
                adCallbackMain?.onAdClick()
                super.onAdOpened()
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private val TAG = "AdmobNative"
    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        adCallback: AdCallback?
    ): Boolean {
        Log.d(TAG, "show: ${layout == null}")
        adCallbackMain = adCallback

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
                unifiedNativeAdView.addView(layoutAds)
                currentUnifiedNativeAd?.let {
                    populateUnifiedNativeAdView(it, unifiedNativeAdView)
                    layout.removeAllViews()
                    layout.addView(unifiedNativeAdView)

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
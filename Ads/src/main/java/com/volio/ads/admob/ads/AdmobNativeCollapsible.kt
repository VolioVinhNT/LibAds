package com.volio.ads.admob.ads

import android.app.ActionBar.LayoutParams
import android.app.Activity
import android.os.Bundle
import android.util.LayoutDirection
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd

import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.R
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.*

class AdmobNativeCollapsible : AdmobAds() {
    var currentUnifiedNativeAd: NativeAd? = null
    private var activity: Activity? = null
    private var adCallbackMain: AdCallback? = null
    private var callbackPreload: PreloadCallback? = null
    private var stateLoadAd = StateLoadAd.NONE

    private var layoutLarge: View? = null

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
        if (adView.mediaView != null && nativeAd.mediaContent != null) {
            adView.mediaView!!.setMediaContent(nativeAd.mediaContent!!)
        }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (adView.callToActionView != null) {
            if (adView.callToActionView != null) {
                if (nativeAd.callToAction == null) {
                    adView.callToActionView!!.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView!!.visibility = View.VISIBLE
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
                adView.iconView!!.visibility = View.INVISIBLE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon!!.drawable
                )
                adView.iconView!!.visibility = View.VISIBLE
            }
        }
        if (adView.priceView != null) {
            if (nativeAd.price == null) {
                adView.priceView!!.visibility = View.INVISIBLE
            } else {
                adView.priceView!!.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }
        }
        if (adView.storeView != null) {
            if (nativeAd.store == null) {
                adView.storeView!!.visibility = View.INVISIBLE
            } else {
                adView.storeView!!.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }
        }
        if (adView.starRatingView != null) {
            if (nativeAd.starRating == null) {
                adView.starRatingView!!.visibility = View.INVISIBLE
            } else {
                (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView!!.visibility = View.VISIBLE
            }
        }
        if (adView.advertiserView != null) {
            if (nativeAd.advertiser == null) {
                adView.advertiserView!!.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView).text = nativeAd.advertiser
                adView.advertiserView!!.visibility = View.VISIBLE
            }
        }

        adView.setNativeAd(nativeAd)

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

        load(activity, idAds, adCallback, loadSuccess = {
            show(activity, idAds, loadingText, layout, layoutAds, lifecycle, adCallback)
        })
    }

    public fun setViewLarge(view: View?) {
        layoutLarge = view
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
        val builder = AdLoader.Builder(activity.applicationContext, id)
        builder.forNativeAd { unifiedNativeAd ->
            Log.d(TAG, "load: ")
            adCallbackMain?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.NATIVE)
            unifiedNativeAd.setOnPaidEventListener {
                kotlin.runCatching {
                    val params = Bundle()
                    params.putString("revenue_micros", it.valueMicros.toString())
                    params.putString("precision_type", it.precisionType.toString())
                    params.putString("ad_unit_id", id)
                    val adapterResponseInfo =
                        unifiedNativeAd?.responseInfo?.loadedAdapterResponseInfo
                    adapterResponseInfo?.let { it ->
                        params.putString("ad_source_id", it.adSourceId)
                        params.putString("ad_source_name", it.adSourceName)
                    }
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

            override fun onAdImpression() {
                super.onAdImpression()
                adCallbackMain?.onAdImpression(AdDef.ADS_TYPE.NATIVE)
//                Firebase.analytics.logEvent(Constant.KeyCustomImpression, Bundle.EMPTY)
            }

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
                Utils.showToastDebug(activity, "id native: ${idAds}")
                adCallbackMain?.onAdClick()
            }

            override fun onAdOpened() {
                super.onAdOpened()
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private val TAG = "AdmobNative"
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
        val isDestroy = lifecycle?.currentState == Lifecycle.State.DESTROYED
        if (isDestroy) {
            adCallback?.onAdFailToShow("Screen destroyed")
            return false
        }

        if (layout != null) {
            val layoutAdsLarge = LayoutInflater.from(activity).inflate(idLayoutLarge, null, false)
            if (layoutAdsLarge != null) {
                val unifiedNativeAdView = NativeAdView(activity)
                unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutAdsLarge.parent?.let {
                    (it as ViewGroup).removeView(layoutAds)
                }
                kotlin.runCatching {
                    layoutAdsLarge.layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                unifiedNativeAdView.addView(layoutAdsLarge)
                currentUnifiedNativeAd?.let {
                    populateUnifiedNativeAdView(it, unifiedNativeAdView)
                    val popupWindow = PopupWindow(
                        unifiedNativeAdView,
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                    popupWindow.animationStyle = R.style.NoAnimation
                    popupWindow.showAsDropDown(layout, 0, -layoutAdsLarge.minimumHeight)

                    // show native small under large native
                    val unifiedNativeAdViewSmall = NativeAdView(activity)
                    unifiedNativeAdViewSmall.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val layoutAdSmall: View = layoutAds
                            ?: LayoutInflater.from(activity)
                                .inflate(R.layout.native_ads_medium_cta_up_shadow, null, false)
                    unifiedNativeAdViewSmall.addView(layoutAdSmall)
                    currentUnifiedNativeAd?.let {
                        populateUnifiedNativeAdView(it, unifiedNativeAdViewSmall)
                        layout.removeAllViews()
                        layout.addView(unifiedNativeAdViewSmall)
                    }
                    val btnClose = layoutAdsLarge.findViewById<View?>(R.id.btnCloseNative)
                    btnClose?.setOnClickListener {
                        popupWindow.dismiss()
                    }
                    lifecycle?.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner, event: Lifecycle.Event
                        ) {
                            if (event == Lifecycle.Event.ON_DESTROY) {
                                popupWindow.dismiss()
                            }
                        }
                    })


                    try {
                        if (it.responseInfo?.adapterResponses?.find { it.adapterClassName == "com.google.ads.mediation.facebook.FacebookMediationAdapter" } != null) {
                            layoutAdsLarge.isSaveEnabled = false
                            layoutAdsLarge.isSaveFromParentEnabled = false

                            unifiedNativeAdView.isSaveEnabled = false
                            unifiedNativeAdView.isSaveFromParentEnabled = false

                            layout.isSaveEnabled = false
                            layout.isSaveFromParentEnabled = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

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


    companion object {
        var idLayoutLarge: Int = R.layout.native_ads_large_collap
    }

}
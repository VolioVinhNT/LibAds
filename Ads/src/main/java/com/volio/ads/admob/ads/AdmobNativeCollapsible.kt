package com.volio.ads.admob.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import com.volio.ads.utils.Utils.showToastDebug
import java.util.Date

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
                                adCallbackMain?.onAdImpression(AdDef.ADS_TYPE.NATIVE_COLLAPSIBLE)
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
                    unifiedNativeAdView.id = R.id.nativeCollapsibleId
                    val parent = layout.parent
                    if (parent is ViewGroup) {
                        parent.findViewById<View>(R.id.nativeCollapsibleId)?.let { ad ->
                            parent.removeView(ad)
                        }
                        when (parent) {
                            is ConstraintLayout -> {
                                val layoutParams = ConstraintLayout.LayoutParams(
                                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.bottomToBottom = layout.id
                                layoutParams.startToStart = layout.id
                                layoutParams.endToEnd = layout.id
                                unifiedNativeAdView.elevation = 10f
                                parent.addView(unifiedNativeAdView, layoutParams)
                            }

                            is FrameLayout -> {
                                val groupLayoutParam =
                                    layout.layoutParams as FrameLayout.LayoutParams
                                val layoutParams = FrameLayout.LayoutParams(
                                    groupLayoutParam.width,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.gravity = groupLayoutParam.gravity
                                layoutParams.leftMargin = groupLayoutParam.leftMargin
                                layoutParams.rightMargin = groupLayoutParam.rightMargin
                                layoutParams.topMargin = groupLayoutParam.topMargin
                                layoutParams.bottomMargin = groupLayoutParam.bottomMargin
                                unifiedNativeAdView.elevation = 10f
                                parent.addView(unifiedNativeAdView, layoutParams)
                            }

                            else -> {
                                showToastDebug(
                                    activity,
                                    "Không hỗ trợ LayoutGroup ${parent::class.java.name}, chỉ hỗ trợ ConstraintLayout và FrameLayout"
                                )
                            }
                        }
                    }

                    // show native small under large native
                    val unifiedNativeAdViewSmall = NativeAdView(activity)
                    unifiedNativeAdViewSmall.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val layoutAdSmall: View = layoutAds
                        ?: LayoutInflater.from(activity)
                            .inflate(R.layout.native_ads_medium_cta_up_shadow, null, false)

                    unifiedNativeAdViewSmall.removeAllViews()
                    (layoutAdSmall.parent as ViewGroup?)?.removeAllViews()
                    try {
                        unifiedNativeAdViewSmall.addView(layoutAdSmall)
                    } catch (e: Exception) {
                    }
                    currentUnifiedNativeAd?.let {
                        populateUnifiedNativeAdView(it, unifiedNativeAdViewSmall)
                        layout.removeAllViews()
                        layout.addView(unifiedNativeAdViewSmall)
                    }
                    val btnClose = layoutAdsLarge.findViewById<View?>(R.id.btnCloseNative)
                    btnClose?.setOnClickListener {
                        val parentGroup = layout.parent
                        if (parentGroup is ViewGroup) {
                            parentGroup.removeView(unifiedNativeAdView)
                        }
                        if (reloadSeconds > 0) {
                            // reload lai native
                            if (lifecycle != null) {
                                load(activity, idAds, adCallback) {}
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (currentUnifiedNativeAd != null) {
                                    if (lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                        show(
                                            activity,
                                            idAds,
                                            loadingText,
                                            layout,
                                            layoutAds,
                                            lifecycle,
                                            adCallback
                                        )
                                    } else if (lifecycle?.currentState != Lifecycle.State.DESTROYED) {
                                        lifecycle?.addObserver(object : LifecycleEventObserver {
                                            override fun onStateChanged(
                                                source: LifecycleOwner,
                                                event: Lifecycle.Event
                                            ) {
                                                if (event == Lifecycle.Event.ON_RESUME) {
                                                    show(
                                                        activity,
                                                        idAds,
                                                        loadingText,
                                                        layout,
                                                        layoutAds,
                                                        lifecycle,
                                                        adCallback
                                                    )
                                                }
                                                if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_DESTROY) {
                                                    lifecycle.removeObserver(this)
                                                }
                                            }

                                        })
                                    }
                                }
                            }, reloadSeconds * 1000L)
                        }
                    }
                    lifecycle?.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner, event: Lifecycle.Event
                        ) {
                            kotlin.runCatching {
                                if (event == Lifecycle.Event.ON_DESTROY) {
                                    val parentGroup = layout.parent
                                    if (parentGroup is ViewGroup) {
                                        parentGroup.removeView(unifiedNativeAdView)
                                    }
                                }
                            }
                        }
                    })
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
        var reloadSeconds: Int = 0
    }

}
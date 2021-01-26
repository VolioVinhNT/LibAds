package com.volio.ads.admob.ads

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.VideoController.VideoLifecycleCallbacks
import com.google.android.gms.ads.formats.MediaView
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import com.volio.ads.AdCallback
import com.volio.ads.R
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*

class AdmobNative : AdmobAds() {
    var currentUnifiedNativeAd: UnifiedNativeAd? = null
    private var activity:Activity? = null
    private var adsChild:AdsChild? = null
    private fun populateUnifiedNativeAdView(nativeAd: UnifiedNativeAd, adView: UnifiedNativeAdView) {
        // Set the media view.
        val viewGroup = adView.findViewById<ViewGroup>(R.id.ad_media)
        if(viewGroup != null) {
            val mediaView = MediaView(adView.context)
            viewGroup.addView(
                mediaView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            adView.mediaView = mediaView
        }else{
            if(Constant.isDebug){
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

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        if(adView.mediaView != null) {
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
        if(adView.callToActionView != null) {
            if (adView.callToActionView != null) {
                if (nativeAd.callToAction == null) {
                    adView.callToActionView.visibility = View.INVISIBLE
                } else {
                    adView.callToActionView.visibility = View.VISIBLE
                    if(adView.callToActionView is Button) {
                        (adView.callToActionView as Button).text = nativeAd.callToAction
                    }else{
                        (adView.callToActionView as TextView).text = nativeAd.callToAction
                    }
                }
            }
        }
        if(adView.iconView != null) {
            if (nativeAd.icon == null) {
                adView.iconView.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon.drawable
                )
                adView.iconView.visibility = View.VISIBLE
            }
        }
        if(adView.priceView != null) {
            if (nativeAd.price == null) {
                adView.priceView.visibility = View.INVISIBLE
            } else {
                adView.priceView.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }
        }
        if(adView.storeView != null) {
            if (nativeAd.store == null) {
                adView.storeView.visibility = View.INVISIBLE
            } else {
                adView.storeView.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }
        }
        if(adView.starRatingView != null) {
            if (nativeAd.starRating == null) {
                adView.starRatingView.visibility = View.INVISIBLE
            } else {
                (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView.visibility = View.VISIBLE
            }
        }
        if(adView.advertiserView != null) {
            if (nativeAd.advertiser == null) {
                adView.advertiserView.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as TextView).text = nativeAd.advertiser
                adView.advertiserView.visibility = View.VISIBLE
            }
        }

        adView.setNativeAd(nativeAd)
        val vc = nativeAd.videoController

        if (vc.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    super.onVideoEnd()
                }
            }
        } else {
        }

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
    override fun preload(activity: Activity,adsChild: AdsChild){
        load(activity,adsChild,null,loadSuccess = {

        })
    }

    private fun load(activity: Activity, adsChild: AdsChild, adCallback: AdCallback?, loadSuccess:()->Unit){
        this.adsChild = adsChild
        this.activity = activity;
        val idAds = if(Constant.isDebug) Constant.ID_ADMOB_NATIVE_TEST else adsChild.adsId
        val builder = AdLoader.Builder(activity.applicationContext, idAds)
        builder.forUnifiedNativeAd { unifiedNativeAd ->
            Log.d(TAG, "load: ")
            adCallback?.onAdShow(AdDef.NETWORK.GOOGLE,AdDef.ADS_TYPE.NATIVE)
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
                Log.d(TAG, "onAdFailedToLoad: ${loadAdError.code} "+loadAdError.message)
                adCallback?.onAdFailToLoad(loadAdError.message)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Utils.showToastDebug(activity,"id native: ${adsChild.adsId}")
                adCallback?.onAdClick()

            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }
    private  val TAG = "AdmobNative"
    override fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        adCallback: AdCallback?
    ): Boolean {
        Log.d(TAG, "show: ${layout == null}")
        if(layout != null) {
            if (layoutAds != null) {
                val unifiedNativeAdView = UnifiedNativeAdView(activity)
                unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                unifiedNativeAdView.addView(layoutAds)
                currentUnifiedNativeAd?.let {
                    populateUnifiedNativeAdView(it, unifiedNativeAdView)
                    layout.removeAllViews()
                    layout.addView(unifiedNativeAdView)

                }
            } else {
                val adView = LayoutInflater.from(activity)
                    .inflate(R.layout.ad_unified, null) as UnifiedNativeAdView
                currentUnifiedNativeAd?.let {
                    populateUnifiedNativeAdView(it, adView)
                    layout.removeAllViews()
                    layout.addView(adView)
                }

            }
            return true
        }else{
            Utils.showToastDebug(activity, "layout ad native not null")
        }
        return false

    }
    public override fun isDestroy():Boolean = (currentUnifiedNativeAd == null)
    public override fun destroy(){
        currentUnifiedNativeAd?.destroy()
        currentUnifiedNativeAd = null;
    }

    public override fun isLoaded(): Boolean {
        return currentUnifiedNativeAd != null
    }

}
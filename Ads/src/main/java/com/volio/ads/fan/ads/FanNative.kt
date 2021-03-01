package com.volio.ads.fan.ads

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Lifecycle
import com.facebook.ads.*

import com.volio.ads.AdCallback
import com.volio.ads.R
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import java.util.*


class FanNative : FanAds() {
    var currentUnifiedNativeAd: NativeAd? = null
    private var activity:Activity? = null
    private var adsChild:AdsChild? = null

    private fun inflateAd(nativeAd: NativeAd,nativeAdLayout:NativeAdLayout) {
        nativeAd.unregisterView()

        // Add the Ad view into the ad container.


        // Add the AdOptionsView
        val adChoicesContainer: LinearLayout = nativeAdLayout.findViewById(R.id.ad_choices_container)
        val adOptionsView =
            AdOptionsView(activity, nativeAd, nativeAdLayout)
        adChoicesContainer.removeAllViews()
        adChoicesContainer.addView(adOptionsView, 0)

        // Create native UI using the ad metadata.
        val viewGroup = nativeAdLayout.findViewById<ViewGroup>(R.id.ad_media)
        var nativeAdMedia: MediaView ? = null
        var nativeAdIcon: MediaView? = null
        if(viewGroup != null) {
            nativeAdMedia = MediaView(nativeAdLayout.context)
            viewGroup.addView(
                nativeAdMedia,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

        }
        val viewGroupIcon = nativeAdLayout.findViewById<View>(R.id.ad_app_icon)
        if(viewGroupIcon != null&&viewGroupIcon is ViewGroup) {
            nativeAdIcon = MediaView(nativeAdLayout.context)
            viewGroupIcon.addView(
                nativeAdIcon,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

        }
        val nativeAdTitle: TextView = nativeAdLayout.findViewById(R.id.ad_headline)

//        val nativeAdSocialContext: TextView =
//            adView.findViewById(android.R.id.native_ad_social_context)
        val nativeAdBody: TextView = nativeAdLayout.findViewById(R.id.ad_body)
        val sponsoredLabel: TextView = nativeAdLayout.findViewById(R.id.ad_sponsored_label)
        val nativeAdCallToAction: TextView =
            nativeAdLayout.findViewById(R.id.ad_call_to_action)


        // Set the Text.
        nativeAdTitle.text = nativeAd.advertiserName
        nativeAdBody.text = nativeAd.adBodyText
//        nativeAdSocialContext.setText(nativeAd.getAdSocialContext())
        nativeAdCallToAction.visibility =
            if (nativeAd.hasCallToAction()) View.VISIBLE else View.INVISIBLE
        nativeAdCallToAction.text = nativeAd.adCallToAction
        sponsoredLabel.text = nativeAd.sponsoredTranslation

        // Create a list of clickable views
        val clickableViews: MutableList<View> = ArrayList()
        clickableViews.add(nativeAdTitle)
        clickableViews.add(nativeAdCallToAction)

        // Register the Title and CTA button to listen for clicks.
        nativeAd.registerViewForInteraction(
            nativeAdLayout, nativeAdMedia, nativeAdIcon, clickableViews
        )
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


    private fun load(activity: Activity, adsChild: AdsChild, adCallback: AdCallback?, loadSuccess:()->Unit) {
        this.adsChild = adsChild
        this.activity = activity
        currentUnifiedNativeAd = NativeAd(activity, adsChild.adsId)

        Log.d(TAG, "load: ${adsChild.adsId}")
        val nativeAdListener: NativeAdListener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad) {
                // Native ad finished downloading all assets
                Log.d(TAG, "onMediaDownloaded: ")
            }

            override fun onError(p0: Ad?, p1: AdError?) {
                Log.d(TAG, "onError: ${p1?.errorMessage}")
                adCallback?.onAdFailToLoad(p1?.errorMessage)
            }


            override fun onAdLoaded(ad: Ad) {
                // Native ad is loaded and ready to be displayed
                Log.d(TAG, "onAdLoaded: ")
                adCallback?.onAdShow(AdDef.NETWORK.FACEBOOK,AdDef.ADS_TYPE.NATIVE)
                loadSuccess()
                timeLoader = Date().time

            }

            override fun onAdClicked(ad: Ad) {
                adCallback?.onAdClick()
                // Native ad clicked
                Log.d(TAG, "onAdClicked: ")
                Utils.showToastDebug(activity,"id native: ${adsChild.adsId}")


            }

            override fun onLoggingImpression(ad: Ad) {
                // Native ad impression
                Log.d(TAG, "onLoggingImpression: ")
            }
        }

        // Request an ad

        // Request an ad
        currentUnifiedNativeAd?.let {
            it.loadAd(
                it.buildLoadAdConfig()
                    .withAdListener(nativeAdListener)
                    .build()
            )
        }
    }
    private  val TAG = "FanNative"
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
                val unifiedNativeAdView = NativeAdLayout(activity)
                unifiedNativeAdView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutAds.parent?.let {
                    (it as ViewGroup).removeView(layoutAds)
                }
                unifiedNativeAdView.addView(layoutAds)
                currentUnifiedNativeAd?.let {
                    layout.removeAllViews()
                    layout.addView(unifiedNativeAdView)
                    inflateAd(it, unifiedNativeAdView)

                }
            } else {
//                val adView = LayoutInflater.from(activity)
//                    .inflate(R.layout.ad_unified, null) as UnifiedNativeAdView
//                currentUnifiedNativeAd?.let {
//                    populateUnifiedNativeAdView(it, adView)
//                    layout.removeAllViews()
//                    layout.addView(adView)
//                }

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
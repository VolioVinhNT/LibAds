package com.volio.ads.admob

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.StateADCallback
import com.volio.ads.admob.ads.*
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.*

private const val TAG = "AdsController"

class AdmobHolder {
    private var hashMap: HashMap<String, AdmobAds> = HashMap()
    public fun loadAndShow(
        activity: Activity,
        isKeepAds: Boolean,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?, failCheck: () -> Boolean
    ) {
        loadAndShow(
            activity,
            adsChild,
            loadingText,
            layout,
            layoutAds,
            lifecycle,
            timeMillisecond,
            object : AdCallback {
                override fun onAdShow(network: String, adtype: String) {
                    Log.d(TAG, "onAdShow: ")
                    if (!isKeepAds) {
                        remove(adsChild)
                    }
                    adCallback?.onAdShow(network, adtype)
                }

                override fun onAdClose(adType: String) {
                    Log.d(TAG, "onAdClose: ")
                    adCallback?.onAdClose(adType)
                }

                override fun onAdFailToLoad(messageError: String?) {
                    Log.d(TAG, "onAdFailToLoad: " + messageError)
                    Utils.showToastDebug(
                        activity,
                        "Admob ${adsChild.adsType} id: ${adsChild.adsId}"
                    )

                    if (!isKeepAds) {
                        remove(adsChild)
                    }
                    if (failCheck()) {
                        adCallback?.onAdFailToLoad(messageError)
                    }
                }

                override fun onAdOff() {
                    Log.d(TAG, "onAdOff: ")
                    adCallback?.onAdOff()
                }

                override fun onAdClick() {
                    adCallback?.onAdClick()
                }

                override fun onPaidEvent(params: Bundle) {
                    adCallback?.onPaidEvent(params)
                }

            })
    }

    private fun loadAndShow(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    ) {
        var ads: AdmobAds? = null
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        when (adsChild.adsType.toLowerCase(Locale.getDefault())) {
            AdDef.ADS_TYPE.NATIVE -> {
                ads = AdmobNative()
            }
            AdDef.ADS_TYPE.INTERSTITIAL -> {
                Log.d("AdmobInterstitial", "loadAndShow: zzz")
                ads = AdmobInterstitial()
            }
            AdDef.ADS_TYPE.BANNER -> {
                ads = AdmobBanner()
            }
            AdDef.ADS_TYPE.BANNER_ADAPTIVE -> {
                ads = AdmobAdaptiveBanner()
            }
            AdDef.ADS_TYPE.REWARD_VIDEO -> {
                ads = AdmobReward()
            }
            AdDef.ADS_TYPE.OPEN_APP -> {
                ads = AdmobOpenAds()
            }
            AdDef.ADS_TYPE.REWARD_INTERSTITIAL -> {
                ads = AdmobRewardInterstitial()
            }
            else -> {
                Utils.showToastDebug(
                    activity,
                    "not support adType ${adsChild.adsType} check file json"
                )
                adCallback?.onAdFailToLoad("")
            }
        }
        Log.d(TAG, "loadAndShow: ${adCallback == null}")

        ads?.loadAndShow(
            activity,
            adsChild,
            loadingText,
            layout,
            layoutAds,
            lifecycle,
            timeMillisecond,
            adCallback
        )
        if (ads != null) hashMap[key] = ads

    }

    public fun preload(
        activity: Activity,
        adsChild: AdsChild,
        preloadCallback: PreloadCallback? = null
    ) {
        var ads: AdmobAds? = null
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        when (adsChild.adsType.toLowerCase(Locale.getDefault())) {
            AdDef.ADS_TYPE.NATIVE -> {
                ads = AdmobNative()
            }
            AdDef.ADS_TYPE.INTERSTITIAL -> {
                ads = AdmobInterstitial()
            }
            AdDef.ADS_TYPE.BANNER -> {
                ads = AdmobBanner()
            }
            AdDef.ADS_TYPE.BANNER_ADAPTIVE -> {
                ads = AdmobAdaptiveBanner()
            }
            AdDef.ADS_TYPE.REWARD_VIDEO -> {
                ads = AdmobReward()
            }
            AdDef.ADS_TYPE.OPEN_APP -> {
                ads = AdmobOpenAds()
            }
            AdDef.ADS_TYPE.REWARD_INTERSTITIAL -> {
                ads = AdmobRewardInterstitial()
            }
            else -> {
                Utils.showToastDebug(
                    activity,
                    "not support adType ${adsChild.adsType} check file json"
                )

            }
        }
        ads?.setPreloadCallback(preloadCallback)
        ads?.preload(activity, adsChild)
        if (ads != null) hashMap[key] = ads
    }

    public fun checkStateAD(
        activity: Activity,
        adsChild: AdsChild,
        stateADCallback: StateADCallback? = null
    ) {
//        var ads: AdmobAds? = null
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        val ads: AdmobAds? = hashMap[key]

        if (ads != null && ads is AdmobInterstitial) {
            try {
                ads.setStateAdCallback(stateADCallback)
            } catch (e: Exception) {
            }
        } else if (ads != null && ads is AdmobReward) {
            try {
                ads.setStateAdCallback(stateADCallback)
            } catch (e: Exception) {
            }
        } else {
            stateADCallback?.onState(StateLoadAd.NONE)
        }


//        when (adsChild.adsType.toLowerCase(Locale.getDefault())) {
//            AdDef.ADS_TYPE.NATIVE -> {
//                ads = AdmobNative()
//            }
//            AdDef.ADS_TYPE.INTERSTITIAL -> {
//                ads = AdmobInterstitial()
//                try {
//                    (ads as AdmobInterstitial).setStateAdCallback(stateADCallback)
//                } catch (e: Exception) {
//                }
//            }
//            AdDef.ADS_TYPE.BANNER -> {
//                ads = AdmobBanner()
//            }
//            AdDef.ADS_TYPE.BANNER_ADAPTIVE -> {
//                ads = AdmobAdaptiveBanner()
//            }
//            AdDef.ADS_TYPE.REWARD_VIDEO -> {
//                ads = AdmobReward()
//            }
//            AdDef.ADS_TYPE.OPEN_APP -> {
//                ads = AdmobOpenAds()
//            }
//            AdDef.ADS_TYPE.REWARD_INTERSTITIAL ->{
//                ads = AdmobRewardInterstitial()
////                try {
////                    (ads as AdmobRewardInterstitial).setPreloadCallback(preloadCallback)
////                } catch (e: Exception) {
////                }
//            }
//            else -> {
//                Utils.showToastDebug(
//                    activity,
//                    "not support adType ${adsChild.adsType} check file json"
//                )
//
//            }
//        }
////        ads?.preload(activity, adsChild)
//        if (ads != null) hashMap[key] = ads
    }


    public fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        timeDelayShowAd: Int = 0,
        adCallback: AdCallback?
    ): Boolean {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        val ads: AdmobAds? = hashMap[key]
        if (ads != null && !ads.isDestroy() && ads.isLoaded() && ads.wasLoadTimeLessThanNHoursAgo(1)) {
            val checkShow = when (adsChild.adsType.toLowerCase(Locale.getDefault())) {
                AdDef.ADS_TYPE.NATIVE,
                AdDef.ADS_TYPE.BANNER,
                AdDef.ADS_TYPE.BANNER_ADAPTIVE,
                AdDef.ADS_TYPE.OPEN_APP,
                AdDef.ADS_TYPE.REWARD_VIDEO,
                AdDef.ADS_TYPE.REWARD_INTERSTITIAL,
                AdDef.ADS_TYPE.INTERSTITIAL -> {
                    ads.show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
                }
                else -> {
                    Utils.showToastDebug(
                        activity,
                        "not support adType ${adsChild.adsType} check file json"
                    )
                    false
                }
            }
//            if(checkShow) hashMap.remove(key)
            return checkShow
        }
        return false
    }

    fun showLoadedAd(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    ) {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        val ads: AdmobAds? = hashMap[key]
        Log.d(
            TAG,
            "showLoadedAd: ${ads != null} ${ads?.isDestroy()} ${ads?.wasLoadTimeLessThanNHoursAgo(1)}"
        )
        if (ads != null && !ads.isDestroy() && ads.wasLoadTimeLessThanNHoursAgo(1)) {
            if (ads.getStateLoadAd() == StateLoadAd.SUCCESS) {
                Log.d(TAG, "showLoadedAd: 1")
                ads.show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
            } else {
                Log.d(TAG, "showLoadedAd: 2")
                if (ads.getStateLoadAd() == StateLoadAd.LOADING) {
                    ads.setPreloadCallback(object : PreloadCallback {
                        override fun onLoadDone() {
                            ads.show(activity, adsChild, loadingText, layout, layoutAds, adCallback)
                        }

                        override fun onLoadFail() {
                            adCallback?.onAdFailToLoad("")
                        }
                    })
                } else {
                    loadAndShow(
                        activity,
                        adsChild,
                        loadingText,
                        layout,
                        layoutAds,
                        lifecycle,
                        timeMillisecond,
                        adCallback
                    )
                }
            }
        } else {
            Log.d(TAG, "showLoadedAd: 3")

            loadAndShow(
                activity,
                adsChild,
                loadingText,
                layout,
                layoutAds,
                lifecycle,
                timeMillisecond,
                adCallback
            )
        }
    }

    public fun destroy(adsChild: AdsChild) {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        hashMap[key]?.destroy()
        Log.d(TAG, "destroy: ${hashMap[key] == null}")

        hashMap.remove(key)
    }

    public fun remove(adsChild: AdsChild) {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        hashMap.remove(key)
    }

    fun getStatusPreload(adsChild: AdsChild): StateLoadAd {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        hashMap[key]?.let {
            return it.getStateLoadAd()
        }
        return StateLoadAd.NULL
    }
}
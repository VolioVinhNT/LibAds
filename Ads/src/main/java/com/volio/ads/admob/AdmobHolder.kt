package com.volio.ads.admob

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdView
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.admob.ads.AdmobAdaptiveBanner
import com.volio.ads.admob.ads.AdmobAds
import com.volio.ads.admob.ads.AdmobBanner
import com.volio.ads.admob.ads.AdmobCollapsibleBanner
import com.volio.ads.admob.ads.AdmobInterstitial
import com.volio.ads.admob.ads.AdmobNative
import com.volio.ads.admob.ads.AdmobNativeCollapsible
import com.volio.ads.admob.ads.AdmobOpenAds
import com.volio.ads.admob.ads.AdmobOpenAdsResume
import com.volio.ads.admob.ads.AdmobReward
import com.volio.ads.admob.ads.AdmobRewardInterstitial
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils

private const val TAG = "AdmobHolder"

class AdmobHolder {
    private var hashMap: HashMap<String, AdmobAds> = HashMap()


    fun loadAndShow(
        activity: Activity,
        isKeepAds: Boolean,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?
    ) {
        loadAndShow(
            activity,
            isKeepAds,
            adsChild,
            loadingText,
            layout,
            layoutAds,
            lifecycle,
            timeMillisecond,
            adCallback,
            0
        )

    }

    private fun loadAndShow(
        activity: Activity,
        isKeepAds: Boolean,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        adCallback: AdCallback?,
        index: Int
    ) {
        val time = System.currentTimeMillis()
        var ads: AdmobAds? = null
        val adId = adsChild.adsIds.sortedBy { it.priority }[index]
        adsChild.adsIds.sortedBy { it.priority }.forEach {
            Log.d(TAG, "loadAndShow: ${it.toString()}")

        }
        when (adsChild.adsType.lowercase()) {
            AdDef.ADS_TYPE.NATIVE_COLLAPSIBLE -> {
                ads = AdmobNativeCollapsible()
            }
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

            AdDef.ADS_TYPE.BANNER_COLLAPSIBLE, AdDef.ADS_TYPE.BANNER_COLLAPSIBLE_TOP -> {
                ads =
                    AdmobCollapsibleBanner(adsChild.adsType.lowercase() == AdDef.ADS_TYPE.BANNER_COLLAPSIBLE)
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

            AdDef.ADS_TYPE.OPEN_APP_RESUME -> {
                ads = AdmobOpenAdsResume()
            }

            else -> {
                Utils.showToastDebug(
                    activity,
                    "not support adType ${adsChild.adsType} check file json"
                )

            }
        }
        ads?.loadAndShow(
            activity,
            adId.id,
            loadingText,
            layout,
            layoutAds,
            lifecycle,
            timeMillisecond,
            object : AdCallback {
                override fun onAdShow(network: String, adtype: String) {
                    adCallback?.onAdShow(network, adtype)
                }

                override fun onAdClose(adType: String) {
                    adCallback?.onAdClose(adType)
                }

                override fun onAdFailToLoad(messageError: String?) {
                    if (index < adsChild.adsIds.size - 1) {
                        if (timeMillisecond != null) {
                            val newTimeout = timeMillisecond - (System.currentTimeMillis() - time)
                            if (newTimeout > 3000) {
                                loadAndShow(
                                    activity,
                                    isKeepAds,
                                    adsChild,
                                    loadingText,
                                    layout,
                                    layoutAds,
                                    lifecycle,
                                    newTimeout,
                                    adCallback,
                                    index + 1
                                )
                            } else {
                                adCallback?.onAdFailToLoad("TimeOut")
                            }
                        } else {
                            loadAndShow(
                                activity,
                                isKeepAds,
                                adsChild,
                                loadingText,
                                layout,
                                layoutAds,
                                lifecycle,
                                null,
                                adCallback,
                                index + 1
                            )
                        }
                    } else {
                        adCallback?.onAdFailToLoad(messageError)
                    }
                }

                override fun onAdFailToShow(messageError: String?) {
                    adCallback?.onAdFailToLoad(messageError)
                }

                override fun onAdOff() {
                    adCallback?.onAdOff()
                }

                override fun onAdClick() {
                    adCallback?.onAdClick()
                }

                override fun onPaidEvent(params: Bundle) {
                    adCallback?.onPaidEvent(params)
                }

                override fun onRewardShow(network: String, adtype: String) {
                    adCallback?.onRewardShow(network, adtype)
                }

                override fun onAdImpression(adType: String) {
                    adCallback?.onAdImpression(adType)
                }

            }
        )
        if (isKeepAds) {
            val key = (adsChild.adsType + adsChild.spaceName + adId.priority)
            if (ads != null) {
                hashMap[key] = ads
            }
        }

    }

    fun clearAllAd() {
        hashMap.clear()
    }

    public fun preload(
        activity: Activity,
        adsChild: AdsChild,
        preloadCallback: PreloadCallback? = null
    ) {
        if (adsChild.adsIds.isNotEmpty()) {
            preload(activity, adsChild, 0, preloadCallback)
        }

    }

    private fun preload(
        activity: Activity,
        adsChild: AdsChild,
        index: Int,
        preloadCallback: PreloadCallback? = null
    ) {
        var ads: AdmobAds? = null
        val adId = adsChild.adsIds.sortedBy { it.priority }[index]
        val key = (adsChild.adsType + adsChild.spaceName + adId.priority)
        when (adsChild.adsType.lowercase()) {
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

            AdDef.ADS_TYPE.BANNER_COLLAPSIBLE, AdDef.ADS_TYPE.BANNER_COLLAPSIBLE_TOP -> {
                ads =
                    AdmobCollapsibleBanner(adsChild.adsType.lowercase() == AdDef.ADS_TYPE.BANNER_COLLAPSIBLE)
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

            AdDef.ADS_TYPE.OPEN_APP_RESUME -> {
                ads = AdmobOpenAdsResume()
            }
            AdDef.ADS_TYPE.NATIVE_COLLAPSIBLE -> {
                ads = AdmobNativeCollapsible()
            }

            else -> {
                Utils.showToastDebug(
                    activity,
                    "not support adType ${adsChild.adsType} check file json"
                )

            }
        }
        ads?.setPreloadCallback(object : PreloadCallback {
            override fun onLoadDone() {
                preloadCallback?.onLoadDone()

            }

            override fun onLoadFail() {
                if (index < adsChild.adsIds.size - 1) {
                    preload(activity, adsChild, index + 1, preloadCallback)
                } else {
                    preloadCallback?.onLoadFail()
                }
            }
        })
        ads?.preload(activity, adId.id)
        if (ads != null) hashMap[key] = ads
    }


    public fun show(
        activity: Activity,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle? = null,
        adCallback: AdCallback?
    ): Boolean {
        adsChild.adsIds.sortedBy { it.priority }.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            val ads: AdmobAds? = hashMap[key]
            if (ads != null && !ads.isDestroy() && ads.isLoaded() &&
                ads.wasLoadTimeLessThanNHoursAgo(1)
            ) {
                val checkShow = when (adsChild.adsType.lowercase()) {
                    AdDef.ADS_TYPE.NATIVE,
                    AdDef.ADS_TYPE.NATIVE_COLLAPSIBLE,
                    AdDef.ADS_TYPE.BANNER,
                    AdDef.ADS_TYPE.BANNER_ADAPTIVE,
                    AdDef.ADS_TYPE.BANNER_COLLAPSIBLE,
                    AdDef.ADS_TYPE.BANNER_COLLAPSIBLE_TOP,
                    AdDef.ADS_TYPE.BANNER_INLINE -> {
                        ads.show(
                            activity,
                            it.id,
                            loadingText,
                            layout,
                            layoutAds,
                            lifecycle,
                            adCallback
                        )
                        return true
                    }

                    AdDef.ADS_TYPE.OPEN_APP,
                    AdDef.ADS_TYPE.REWARD_VIDEO,
                    AdDef.ADS_TYPE.REWARD_INTERSTITIAL,
                    AdDef.ADS_TYPE.INTERSTITIAL,
                    AdDef.ADS_TYPE.OPEN_APP_RESUME -> {
                        val check = ads.show(
                            activity,
                            it.id,
                            loadingText,
                            layout,
                            layoutAds,
                            lifecycle,
                            adCallback
                        )
                        if (check) {
                            hashMap.remove(key)
                        }
                        check
                    }

                    else -> {
                        Utils.showToastDebug(
                            activity,
                            "not support adType ${adsChild.adsType} check file json"
                        )
                        Log.d(TAG, "show not support adType: ${adsChild.adsType}")
                        false
                    }
                }
                if (checkShow) {
                    return true
                }
            } else {
                adCallback?.onAdFailToLoad("more than one hour")
                destroy(adsChild)
            }
        }

        return false
    }

    private fun showAds() {

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
        val list = adsChild.adsIds.sortedBy { it.priority }
        var adsSuccess: AdmobAds? = null
        var adsLoading: AdmobAds? = null
        var idAdsSuccess = ""
        var idAdsLoading = ""
        list.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            val ads = hashMap[key]
            if (adsSuccess == null && ads?.getStateLoadAd() == StateLoadAd.SUCCESS) {
                adsSuccess = ads
                idAdsSuccess = it.id
            }
            if (adsLoading == null && ads?.getStateLoadAd() == StateLoadAd.LOADING) {
                adsLoading = ads
                idAdsLoading = it.id
            }
        }

        Log.e(
            TAG,
            "showLoadedAd waiting: ${adsLoading != null} ${adsLoading?.isDestroy() != true} ${
                adsLoading?.wasLoadTimeLessThanNHoursAgo(1) == true
            }",
        )
        Log.e(TAG, "showLoadedAd sucess: ${adsSuccess != null} ")
        if (adsSuccess != null) {
            adsSuccess?.show(
                activity,
                idAdsSuccess,
                loadingText,
                layout,
                layoutAds,
                lifecycle,
                adCallback
            )

            Log.e(TAG, "showLoadedAd: success ${idAdsSuccess}")
        } else if (adsLoading != null) {
            Log.e(TAG, "showLoadedAd: wait loading.. ${idAdsLoading}")
            adsLoading?.setPreloadCallback(object : PreloadCallback {
                override fun onLoadDone() {
                    adsLoading?.show(
                        activity,
                        idAdsLoading,
                        loadingText,
                        layout,
                        layoutAds,
                        lifecycle,
                        adCallback
                    )
                    Log.e(TAG, "showLoadedAd: wait suceess")
                }

                override fun onLoadFail() {
                    adCallback?.onAdFailToLoad("")
                    Log.e(TAG, "showLoadedAd: wait failed")
                }
            })
        } else {
            Log.e(TAG, "showLoadedAd: load and show ${adsChild.adsIds}")
            loadAndShow(
                activity,
                true,
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
        adsChild.adsIds.sortedBy { it.priority }.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            hashMap[key]?.destroy()
            hashMap.remove(key)
        }
    }

    public fun remove(adsChild: AdsChild) {
        adsChild.adsIds.sortedBy { it.priority }.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            hashMap.remove(key)
        }
    }

    fun getStatusPreload(adsChild: AdsChild): StateLoadAd {
        var stateLoadAd: StateLoadAd = StateLoadAd.NULL
        adsChild.adsIds.sortedBy { it.priority }.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            val ads = hashMap[key]
            if (ads != null) {
                Log.d(TAG, "getStatusPreload0: ${ads.getStateLoadAd()}")
                when (ads.getStateLoadAd()) {
                    StateLoadAd.LOADING, StateLoadAd.HAS_BEEN_OPENED, StateLoadAd.SUCCESS -> {
                        return ads.getStateLoadAd()
                    }

                    else -> {
                        stateLoadAd = ads.getStateLoadAd()
                    }
                }
            }

        }
        Log.d(TAG, "getStatusPreload1: $stateLoadAd")
        return stateLoadAd
    }

    fun getAdView(adsChild: AdsChild): AdmobAds? {

        adsChild.adsIds.sortedBy { it.priority }.forEach {
            val key = (adsChild.adsType + adsChild.spaceName + it.priority)
            val ads = hashMap[key]
            if (ads != null) {
                if (ads is AdmobCollapsibleBanner) {
                    return ads
                }
            }
        }
        return null
    }
}
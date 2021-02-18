package com.volio.ads.fan

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.volio.ads.AdCallback
import com.volio.ads.admob.ads.*
import com.volio.ads.fan.ads.FanAds
import com.volio.ads.fan.ads.FanBanner
import com.volio.ads.fan.ads.FanInterstitial
import com.volio.ads.fan.ads.FanNative
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Utils
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "AdsController2"

class FanHolderHolder {
    private var hashMap: HashMap<String, AdmobAds> = HashMap()
    private var hashMapFan: HashMap<String, FanAds> = HashMap()

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
                        adCallback?.onAdShow(network, adtype)
                        Log.d(TAG, "onAdShow: ")
                        remove(adsChild)

                    }

                    override fun onAdClose(adType: String) {
                        Log.d(TAG, "onAdClose: ")
                        adCallback?.onAdClose(adType)
                    }

                    override fun onAdFailToLoad(messageError: String?) {
                        Log.d(TAG, "onAdFailToLoad: " + messageError)
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
        var ads: FanAds? = null
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        when (adsChild.adsType.toLowerCase(Locale.getDefault())) {
            AdDef.ADS_TYPE.NATIVE -> {
                ads = FanNative()
            }
            AdDef.ADS_TYPE.INTERSTITIAL -> {
                ads = FanInterstitial()
            }
            AdDef.ADS_TYPE.BANNER -> {
                ads = FanBanner()
            }
//            AdDef.ADS_TYPE.BANNER_ADAPTIVE -> {
//                ads = AdmobAdaptiveBanner()
//            }
//            AdDef.ADS_TYPE.REWARD_VIDEO -> {
//                ads = AdmobReward()
//            }
//            AdDef.ADS_TYPE.OPEN_APP -> {
//                ads = AdmobOpenAds()
//            }
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
        if (ads != null) hashMapFan[key] = ads

    }

    public fun preload(activity: Activity, adsChild: AdsChild) {
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
            else -> {
                Utils.showToastDebug(
                        activity,
                        "not support adType ${adsChild.adsType} check file json"
                )

            }
        }
        ads?.preload(activity, adsChild)
        if (ads != null) hashMap[key] = ads
    }


    public fun show(
            activity: Activity,
            adsChild: AdsChild,
            loadingText: String?,
            layout: ViewGroup?,
            layoutAds: View?,
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
            if(checkShow) hashMap.remove(key)
            return checkShow
        }
        return false
    }

    public fun destroy(adsChild: AdsChild) {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        hashMap[key]?.destroy()
        hashMap.remove(key)
    }

    public fun remove(adsChild: AdsChild) {
        val key = (adsChild.adsType + adsChild.spaceName).toLowerCase(Locale.getDefault())
        hashMap.remove(key)
    }
}
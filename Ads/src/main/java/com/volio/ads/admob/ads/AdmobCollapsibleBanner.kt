package com.volio.ads.admob.ads

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.volio.ads.AdCallback
import com.volio.ads.PreloadCallback
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.StateLoadAd
import com.volio.ads.utils.Utils
import java.util.Date


class AdmobCollapsibleBanner(private val isShowBottom: Boolean = true) : AdmobAds() {
    private var isLoadSuccess = false
    private var adView: AdView? = null
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
                adView?.adListener = object : AdListener() {
                    override fun onAdClicked() {
                        super.onAdClicked()
                        callback?.onAdClick()
                        Utils.showToastDebug(activity, "Admob CollapsibleBanner: $idAds")
                    }



                    override fun onAdOpened() {
                        super.onAdOpened()

                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        callback?.onAdClose(AdDef.NETWORK.GOOGLE)
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        Utils.showToastDebug(activity, "Admob CollapsibleBanner: ${p0.message}")
                        callback?.onAdFailToLoad(p0.message)
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.e("TAG", "onAdImpression: ")
                        callback?.onAdImpression(AdDef.ADS_TYPE.BANNER_COLLAPSIBLE)
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        isLoadSuccess = true

                        timeLoader = Date().time
                    }
                }


                callback?.onAdShow(AdDef.NETWORK.GOOGLE, AdDef.ADS_TYPE.BANNER)
                layout.removeAllViews()
                if (adView!!.parent != null) {
                    (adView!!.parent as ViewGroup).removeView(adView) // <- fix
                }
                layout.addView(adView)
                lifecycle?.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(
                        source: LifecycleOwner, event: Lifecycle.Event
                    ) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            adView?.destroy()
                            layout.removeAllViews()
                            if (adView!!.parent != null) {
                                (adView!!.parent as ViewGroup).removeView(adView)
                            }
                            adView = null
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
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
        layout?.post {
            callback = adCallback
            val id: String = if (Constant.isDebug) {
                Constant.ID_ADMOB_BANNER_COLLAPSIVE_TEST
            } else {
                idAds
            }
            stateLoadAd = StateLoadAd.LOADING
            isLoadSuccess = false
            adView = AdView(activity)
            adView?.setBackgroundColor(Color.WHITE)
            adView?.adUnitId = id


            val adSize = getAdsize(activity)
            adSize?.let {
                adView?.setAdSize(it)
            }

            layout?.let { viewG ->
                val lp = viewG.layoutParams
                lp.width = adSize?.getWidthInPixels(viewG.context) ?: 0
                lp.height = adSize?.getHeightInPixels(viewG.context) ?: 0
                viewG.layoutParams = lp
            }

            Log.d("dsk6", "BANNER_COLLAPSIBLE: ")
            val extras = Bundle()

            if (isShowBottom){
                extras.putString("collapsible", "bottom")
            } else {
                extras.putString("collapsible", "top")
            }
            adView?.loadAd(
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            )



            adView?.adListener = object : AdListener() {

                override fun onAdClicked() {
                    super.onAdClicked()
                    callback?.onAdClick()
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Utils.showToastDebug(activity, "Admob AdapBanner: ${id}")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    callback?.onAdClose(AdDef.NETWORK.GOOGLE)
                }


                override fun onAdImpression() {
                    super.onAdImpression()
                    callback?.onAdImpression(AdDef.ADS_TYPE.BANNER_ADAPTIVE)
                    Log.e("TAG", "onAdImpression: ")
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Utils.showToastDebug(activity, "Admob AdapBanner: ${p0?.message}")
                    callback?.onAdFailToLoad(p0?.message)
                    stateLoadAd = StateLoadAd.FAILED
                    callbackPreload?.onLoadFail()
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adView?.onPaidEventListener = OnPaidEventListener {
                        kotlin.runCatching {
                            val params = Bundle()
                            params.putString("revenue_micros", it.valueMicros.toString())
                            params.putString("precision_type", it.precisionType.toString())
                            params.putString("ad_unit_id", adView?.adUnitId)
                            val adapterResponseInfo =
                                adView?.responseInfo?.loadedAdapterResponseInfo
                            adapterResponseInfo?.let { it ->
                                params.putString("ad_source_id", it.adSourceId)
                                params.putString("ad_source_name", it.adSourceName)
                            }
                            callback?.onPaidEvent(params)
                        }
                    }
                    stateLoadAd = StateLoadAd.SUCCESS
                    isLoadSuccess = true
                    callbackPreload?.onLoadDone()
                    loadSuccess()
                    timeLoader = Date().time
                }
            }
        }

    }

    private fun getAdsize(activity: Activity): AdSize? {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            activity, adWidth
        )
    }

    override fun destroy() {
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
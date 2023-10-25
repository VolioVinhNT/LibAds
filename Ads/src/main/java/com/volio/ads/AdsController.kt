package com.volio.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.adcolony.sdk.AdColonyAdViewActivity
import com.adcolony.sdk.AdColonyInterstitialActivity
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.api.PurchaseClient
import com.appsflyer.api.Store
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.volio.ads.admob.AdmobHolder
import com.volio.ads.model.Ads
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.*
import com.volio.ads.utils.Utils.showToastDebug
import java.util.*

private const val TAG = "AdsController"

class AdsController private constructor(
    application: Application,
    private var appId: String,
    private var packetName: String,
    private var pathJson: String,
    private var isUseAppflyer :Boolean= true
) {
    private var gson = Gson()
    private val hashMapAds: HashMap<String, AdsChild> = hashMapOf()
    private val admobHolder = AdmobHolder()
    private var adsOpenResume: AdsChild? = null
    private var lastTimeClickAds = 0L
    private var isShowOpenAdsResumeNextTime = true

    var isPremium: Boolean = false
    var isTrackAdRevenue = true
    var isAutoShowAdsResume: Boolean = true
    var adCallbackAll: AdCallback? = null


    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var adsController: AdsController

        @SuppressLint("StaticFieldLeak")
        var activity: Activity? = null

        @SuppressLint("StaticFieldLeak")
        var adActivity: Activity? = null

        fun init(
            application: Application,
            isDebug: Boolean,
            appId: String,
            packetName: String,
            pathJson: String,
            isUseAppFlyer: Boolean = true
        ) {
            fun checkAdActivity(activity: Activity): Boolean {
                return activity is AdActivity ||
                        activity is com.vungle.warren.AdActivity ||
                        activity is AdColonyInterstitialActivity ||
                        activity is AdColonyAdViewActivity
            }

            fun setActivity(activity: Activity) {
                if (checkAdActivity(activity)) {
                    adActivity = activity
                } else if (activity is AppCompatActivity) {
                    this@Companion.activity = activity
                }
            }

            Constant.isDebug = isDebug
            MobileAds.initialize(application)
            adsController = AdsController(application, appId, packetName, pathJson,isUseAppFlyer)

            application.registerActivityLifecycleCallbacks(object :
                Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    setActivity(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    setActivity(activity)
                    if (!checkAdActivity(activity)) {
                        adsController.showAdsResume()
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    setActivity(activity)
                }

                override fun onActivityPaused(activity: Activity) {

                }

                override fun onActivityStopped(activity: Activity) {

                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (checkAdActivity(activity)) {
                        adActivity = null
                    }
                }
            })
        }

        fun getInstance(): AdsController {
            if (!::adsController.isInitialized) {
                throw Throwable("call init")
            }
            return adsController
        }

        fun checkInit() = ::adsController.isInitialized
    }


    fun setDebugMode(isDebug: Boolean) {
        Constant.isDebug = isDebug
    }

    fun setShowOpenAdsNextSession(isShow: Boolean) {
        isShowOpenAdsResumeNextTime = isShow
    }

    private fun initAppFlyer(application: Application) {
        val afRevenueBuilder = AppsFlyerAdRevenue.Builder(application)
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build())
        AppsFlyerLib.getInstance().init("4Ti9yuyaVb6BJMoy25gWUP", object :
            AppsFlyerConversionListener {
            override fun onConversionDataSuccess(p0: MutableMap<String, Any>?) {
            }

            override fun onConversionDataFail(p0: String?) {
            }

            override fun onAppOpenAttribution(p0: MutableMap<String, String>?) {
            }

            override fun onAttributionFailure(p0: String?) {
            }
        }, application)
        AppsFlyerLib.getInstance().setCollectAndroidID(true)
        AppsFlyerLib.getInstance().setCollectIMEI(true)
        AppsFlyerLib.getInstance().setCollectOaid(true)

//        AppsFlyerLib.getInstance().setDebugLog(true)
        AppsFlyerLib.getInstance().start(application)
        // init
        val builder = PurchaseClient.Builder(application, Store.GOOGLE)
        // Make sure to keep this instance
        val afPurchaseClient = builder.build()
        // start
        afPurchaseClient.startObservingTransactions()
        builder.logSubscriptions(true)
        builder.autoLogInApps(true)
    }

    fun getDebugMode() = Constant.isDebug
    private fun checkAppIdPacket(ads: Ads): Boolean {
        var checkAppId = false
        var checkPacket = false
        if (ads.appId == appId) {
            checkAppId = true
        }

        if (!checkAppId) showToastDebug(activity, "wrong appId network ${ads.network}")

        if (ads.packetName != packetName) {
            showToastDebug(activity, "wrong packetName")
        } else {
            checkPacket = true
        }
        return checkAppId && checkPacket
    }

    init {
        AudienceNetworkInitializeHelper.initialize(application)
        if (isUseAppflyer) {
            initAppFlyer(application)
        }

        try {
            val data = Utils.getStringAssetFile(pathJson, application)
            Log.d(TAG, ": $data")
            val ads = gson.fromJson(data, Ads::class.java)
            if (checkAppIdPacket(ads)) {
                ads.listAdsChild.forEach {
                    if (it.adsType.lowercase() == AdDef.ADS_TYPE.OPEN_APP_RESUME) {
                        adsOpenResume = it
                    }
                    hashMapAds[it.spaceName] = it
                }
            }
        } catch (e: Exception) {
            showToastDebug(activity, "no load data json ads file")
        }


    }

    private fun showAdsResume() {
        if (isAutoShowAdsResume && !isPremium) {
            if (isShowOpenAdsResumeNextTime) {
                activity?.let {
                    adsOpenResume?.let { adsChild ->
                        val checkShow = admobHolder.show(
                            it,
                            adsChild,
                            null,
                            null,
                            null,
                            null,
                            getAdCallback(adsChild, object : AdCallback {
                                override fun onAdShow(network: String, adtype: String) {}
                                override fun onAdClose(adType: String) {
                                    preloadAdsResume()
                                }

                                override fun onAdFailToLoad(messageError: String?) {}
                                override fun onAdOff() {}

                            })
                        )
                        if (checkShow) {
                            isShowOpenAdsResumeNextTime = false
                        } else {
                            preloadAdsResume()
                        }
                    }
                }
            }
            if (System.currentTimeMillis() - lastTimeClickAds > 1000) {
                isShowOpenAdsResumeNextTime = true
            }
        }

    }

    fun preloadAdsResume() {
        if (!isPremium) {
            activity?.let {
                isAutoShowAdsResume = true
                adsOpenResume?.let { adsChild ->
                    val status = admobHolder.getStatusPreload(adsChild)
                    if (status != StateLoadAd.LOADING && status != StateLoadAd.SUCCESS) {
                        admobHolder.preload(it, adsChild)
                    }
                }
            }
        }
    }


    fun checkAdsNotShowOpenResume(adsChild: AdsChild): Boolean {
        when (adsChild.adsType.lowercase()) {
            AdDef.ADS_TYPE.INTERSTITIAL,
            AdDef.ADS_TYPE.REWARD_VIDEO,

            AdDef.ADS_TYPE.OPEN_APP,

            AdDef.ADS_TYPE.REWARD_INTERSTITIAL -> {
                return true
            }
        }
        return false
    }

    private fun getAdCallback(adsChild: AdsChild, adCallback: AdCallback?): AdCallback {
        return object : AdCallback {
            override fun onAdShow(network: String, adtype: String) {
                adCallback?.onAdShow(network, adtype)
                adCallbackAll?.onAdShow(network, adtype)
            }

            override fun onAdClose(adType: String) {
                adCallback?.onAdClose(adType)
                adCallbackAll?.onAdClose(adType)
                if (checkAdsNotShowOpenResume(adsChild) && System.currentTimeMillis() - lastTimeClickAds > 1000) {
                    isShowOpenAdsResumeNextTime = true
                }
            }

            override fun onAdFailToLoad(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(messageError)
                isShowOpenAdsResumeNextTime = true

            }

            override fun onAdFailToShow(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(messageError)
                isShowOpenAdsResumeNextTime = true

            }

            override fun onAdOff() {
                adCallback?.onAdOff()
                adCallbackAll?.onAdOff()
            }

            override fun onAdClick() {
                adCallback?.onAdClick()
                adCallbackAll?.onAdClick()
                isShowOpenAdsResumeNextTime = false
                lastTimeClickAds = System.currentTimeMillis()
            }

            override fun onPaidEvent(params: Bundle) {
                adCallback?.onPaidEvent(params)
                adCallbackAll?.onPaidEvent(params)
                if (isUseAppflyer) {
                    AppFlyerUtils.logAdRevenue(params)
                }
            }

            override fun onRewardShow(network: String, adtype: String) {
                adCallback?.onRewardShow(network, adtype)
                adCallbackAll?.onRewardShow(network, adtype)
            }

            override fun onAdImpression(adType: String) {
                adCallback?.onAdImpression(adType)
                adCallbackAll?.onAdImpression(adType)
            }
        }
    }

    fun preload(spaceName: String, preloadCallback: PreloadCallback? = null) {
        if (!isPremium) {
            activity?.let {
                hashMapAds[spaceName]?.let { ads ->
                    admobHolder.preload(it, ads, preloadCallback)
                }
            }
        }

    }


    fun showLoadedAd(
        spaceName: String,
        loadingText: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? = null
    ) {
        if (!isPremium) {
            activity?.let {
                hashMapAds[spaceName]?.let { ads ->
                    if (checkAdsNotShowOpenResume(ads)) {
                        isShowOpenAdsResumeNextTime = false
                    }
                    admobHolder.showLoadedAd(
                        it,
                        ads,
                        loadingText,
                        layout,
                        layoutAds,
                        lifecycle,
                        timeMillisecond,
                        getAdCallback(ads, adCallback)
                    )
                }
            }
        }
    }


    fun show(
        spaceName: String,
        textLoading: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? = null
    ) {
        if (!isPremium) {
            activity?.let {
                hashMapAds[spaceName]?.let { ads ->
                    if (checkAdsNotShowOpenResume(ads)) {
                        isShowOpenAdsResumeNextTime = false
                    }
                    admobHolder.show(
                        it,
                        ads,
                        textLoading,
                        layout,
                        layoutAds,
                        lifecycle,
                        getAdCallback(ads, adCallback)
                    )
                }
            }
        }

    }

    fun loadAndShow(
        spaceName: String,
        isKeepAds: Boolean = false,
        loadingText: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long? = null,
        adCallback: AdCallback? = null
    ) {
        if (!isPremium) {
            activity?.let {
                hashMapAds[spaceName]?.let { ads ->
                    if (checkAdsNotShowOpenResume(ads)) {
                        isShowOpenAdsResumeNextTime = false
                    }
                    admobHolder.loadAndShow(
                        it,
                        isKeepAds,
                        ads,
                        loadingText,
                        layout,
                        layoutAds,
                        lifecycle,
                        timeMillisecond,
                        getAdCallback(ads, adCallback)
                    )
                }
            }
        }
    }


    fun getStatusPreload(spaceName: String): StateLoadAd {
        val item = hashMapAds[spaceName]
        if (item != null) {
            return admobHolder.getStatusPreload(item)
        }
        return StateLoadAd.NONE
    }


    fun destroy(spaceName: String) {
        val adsChild = hashMapAds[spaceName]
        adsChild?.let { admobHolder.destroy(it) }
    }

}
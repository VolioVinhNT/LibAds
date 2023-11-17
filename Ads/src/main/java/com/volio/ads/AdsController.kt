package com.volio.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import com.volio.ads.model.AdsId
import com.volio.ads.utils.*
import com.volio.ads.utils.AdDef.ADS_TYPE.Companion.INTERSTITIAL
import com.volio.ads.utils.Utils.showToastDebug
import java.util.*

private const val TAG = "AdsController"

class AdsController private constructor(
    application: Application,
    private var appId: String,
    private var packetName: String,
    private var pathJson: String,
    private var isUseAppflyer: Boolean = true
) {
    private var gson = Gson()
    private val hashMapAds: HashMap<String, AdsChild> = hashMapOf()
    private val admobHolder = AdmobHolder()
    private var adsOpenResume: AdsChild? = null
    private var lastTimeClickAds = 0L
    private var isShowOpenAdsResumeNextTime = true
    private var isShowAdsFullScreen = false
    private var isAutoShowAdsResume: Boolean = false
    private var lastTimeShowOpenAds: Long = 0L

    var isPremium: Boolean = false
    var isTrackAdRevenue = true
    var adCallbackAll: AdCallbackAll? = null

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
                return activity is AdActivity || activity is com.vungle.warren.AdActivity || activity is AdColonyInterstitialActivity || activity is AdColonyAdViewActivity
            }

            fun setActivity(activity: Activity) {
                if (checkAdActivity(activity)) {
                    adActivity = activity
                } else if (activity is AppCompatActivity || activity is ComponentActivity) {
                    this@Companion.activity = activity
                }
            }

            Constant.isDebug = isDebug
            MobileAds.initialize(application)
            adsController = AdsController(application, appId, packetName, pathJson, isUseAppFlyer)

            application.registerActivityLifecycleCallbacks(object :
                Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    setActivity(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    setActivity(activity)
                    if (activity is AppCompatActivity || activity is ComponentActivity) {
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
        AppsFlyerLib.getInstance()
            .init(AppFlyerUtils.keyAppFlyer, object : AppsFlyerConversionListener {
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
        if (isAutoShowAdsResume && !isPremium && !isShowAdsFullScreen) {
            if (isShowOpenAdsResumeNextTime) {
                if (System.currentTimeMillis() - lastTimeShowOpenAds > 5000) {
                    activity?.let {
                        adsOpenResume?.let { adsChild ->
                            isShowAdsFullScreen = true
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
                                lastTimeShowOpenAds = System.currentTimeMillis()
                                isShowOpenAdsResumeNextTime = false
                            } else {
                                isShowAdsFullScreen = false
                                preloadAdsResume()
                            }
                        }
                    }
                }
            } else {
                lastTimeShowOpenAds = System.currentTimeMillis()
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


    private fun checkAdsNotShowOpenResume(adsChild: AdsChild): Boolean {
        when (adsChild.adsType.lowercase()) {
            AdDef.ADS_TYPE.INTERSTITIAL, AdDef.ADS_TYPE.REWARD_VIDEO,

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
                adCallbackAll?.onAdShow(adsChild)
            }

            override fun onAdClose(adType: String) {
                adCallback?.onAdClose(adType)
                adCallbackAll?.onAdClose(adsChild)
                isShowAdsFullScreen = false
            }

            override fun onAdFailToLoad(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(adsChild, messageError)
                showToastDebug(
                    "Fail to load ${adsChild.adsType} ${adsChild.spaceName}",
                    adsChild.adsIds
                )
            }

            override fun onAdFailToShow(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(adsChild, messageError)
                isShowAdsFullScreen = false
                showToastDebug(
                    "Fail to show ${adsChild.adsType} ${adsChild.spaceName}",
                    adsChild.adsIds
                )
            }

            override fun onAdOff() {
                adCallback?.onAdOff()
                adCallbackAll?.onAdOff(adsChild)
            }

            override fun onAdClick() {
                adCallback?.onAdClick()
                adCallbackAll?.onAdClick(adsChild)
                isShowOpenAdsResumeNextTime = false
                lastTimeClickAds = System.currentTimeMillis()
                showToastDebug("Click ${adsChild.adsType} ${adsChild.spaceName}", adsChild.adsIds)
            }

            override fun onPaidEvent(params: Bundle) {
                adCallback?.onPaidEvent(params)
                adCallbackAll?.onPaidEvent(adsChild, params)
                if (isUseAppflyer) {
                    AppFlyerUtils.logAdRevenue(params)
                }
            }

            override fun onRewardShow(network: String, adtype: String) {
                adCallback?.onRewardShow(network, adtype)
                adCallbackAll?.onRewardShow(adsChild)
            }

            override fun onAdImpression(adType: String) {
                adCallback?.onAdImpression(adType)
                adCallbackAll?.onAdImpression(adsChild)
            }
        }
    }

    fun preload(spaceName: String, preloadCallback: PreloadCallback? = null) {
        if (!isPremium) {
            activity?.let {
                hashMapAds[spaceName]?.let { ads ->
                    if (ads.adsType == INTERSTITIAL || ads.adsType == AdDef.ADS_TYPE.OPEN_APP || ads.adsType == AdDef.ADS_TYPE.REWARD_VIDEO) {
                        showToastDebug("Pre load ${ads.adsType} ${ads.spaceName}", ads.adsIds)
                    }
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
                        isShowAdsFullScreen = true
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
                } ?: kotlin.run {
                    showToastDebug("Không tìm thấy space: $spaceName", listOf())
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
                        isShowAdsFullScreen = true
                    }
                    val status = admobHolder.show(
                        it,
                        ads,
                        textLoading,
                        layout,
                        layoutAds,
                        lifecycle,
                        getAdCallback(ads, adCallback)
                    )
                    if (!status) isShowAdsFullScreen = false
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
                        isShowAdsFullScreen = true
                    }
                    if (ads.adsType == INTERSTITIAL || ads.adsType == AdDef.ADS_TYPE.OPEN_APP || ads.adsType == AdDef.ADS_TYPE.REWARD_VIDEO) {
                        showToastDebug("Load ${ads.adsType} ${ads.spaceName}", ads.adsIds)
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


    private fun showToastDebug(title: String, list: List<AdsId>) {
        if (Constant.isDebug || Constant.isShowToastDebug) {
            try {
                val popupView: View =
                    LayoutInflater.from(activity).inflate(R.layout.pop_up_toast, null)
                val tvTitle = popupView.findViewById<TextView>(R.id.tvTitle)
                val tv1 = popupView.findViewById<TextView>(R.id.tv1)
                val tv2 = popupView.findViewById<TextView>(R.id.tv2)
                val tv3 = popupView.findViewById<TextView>(R.id.tv3)
                tvTitle.text = title
                val listSort = list.sortedBy { it.priority }

                when (list.size) {
                    1 -> {
                        tv1.visibility = View.VISIBLE
                        tv1.text = listSort.first().id
                    }
                    2 -> {
                        tv1.visibility = View.VISIBLE
                        tv3.visibility = View.VISIBLE
                        tv1.text = listSort[0].id
                        tv3.text = listSort[1].id
                    }
                    3 -> {
                        tv1.visibility = View.VISIBLE
                        tv2.visibility = View.VISIBLE
                        tv3.visibility = View.VISIBLE
                        tv1.text = listSort[0].id
                        tv2.text = listSort[1].id
                        tv3.text = listSort[2].id
                    }
                    else -> {

                    }
                }

                Toast(activity).apply {
                    view = popupView
                    duration = Toast.LENGTH_LONG
                    setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
                    show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
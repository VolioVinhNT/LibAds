package com.volio.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.appsflyer.internal.models.InAppPurchaseValidationResult
import com.appsflyer.internal.models.SubscriptionValidationResult
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.gson.Gson
import com.volio.ads.admob.AdmobHolder
import com.volio.ads.model.Ads
import com.volio.ads.model.AdsChild
import com.volio.ads.model.AdsId
import com.volio.ads.utils.*
import com.volio.ads.utils.AdDef.ADS_TYPE.Companion.INTERSTITIAL
import com.volio.ads.utils.Constant.ERROR_AD_OFF
import com.volio.ads.utils.Constant.isDebug
import com.volio.ads.utils.Utils.showToastDebug
import com.volio.cmp.CMPCallback
import com.volio.cmp.CMPController
import java.io.File
import java.io.FileReader
import java.util.*

private const val TAG = "AdsController"

class AdsController private constructor(
    private val application: Application,
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
    private var isWaitCMP: Boolean = true
    private var listRunnable: MutableList<Runnable> = mutableListOf()

    var isPremium: Boolean = false
    var isTrackAdRevenue = true
    var adCallbackAll: AdCallbackAll? = null
    private var isShowCMP = false

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
            isUseAppFlyer: Boolean = true,
            isAutoShowCMP: Boolean = true
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
            Log.d("dsk3", "isDebug: $isDebug")
            MobileAds.initialize(application)
            adsController = AdsController(application, appId, packetName, pathJson, isUseAppFlyer)

            application.registerActivityLifecycleCallbacks(object :
                Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    setActivity(activity)
                    if (isAutoShowCMP && !adsController.isShowCMP) {
                        adsController.isShowCMP = true
                        CMPController(activity).showCMP(isDebug,
                            object : CMPCallback {
                                override fun onShowAd() {

                                }

                                override fun onChangeScreen() {

                                }
                            })
                    }
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

    fun setEnableAppResume(isEnable: Boolean) {
        isAutoShowAdsResume = isEnable
    }


    fun setTestDevice(idDevice: String) {
        if (BuildConfig.DEBUG && isDebug) {
            val testDeviceIds = listOf(idDevice)
            val configuration =
                RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)
        }
    }

    public fun initAppFlyer(application: Application) {
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
//        val builder = PurchaseClient.Builder(application, Store.GOOGLE)
//        // Make sure to keep this instance
//        val afPurchaseClient = builder.build()
//        // start
//        afPurchaseClient.startObservingTransactions()
//        builder.logSubscriptions(true)
//        builder.autoLogInApps(true)

        val afPurchaseClient = PurchaseClient.Builder(application, Store.GOOGLE)
            // Enable Subscriptions auto logging
            .logSubscriptions(true)
            // Enable In Apps auto logging
            .autoLogInApps(true)
            // set production environment
            .setSandbox(true)
            // Subscription Purchase Event Data source listener. Invoked before sending data to AppsFlyer servers
            // to let customer add extra parameters to the payload
            .setSubscriptionPurchaseEventDataSource {
                mapOf(
                    "some key" to "some value", "another key" to it.size
                )
            }
            // In Apps Purchase Event Data source listener. Invoked before sending data to AppsFlyer servers
            // to let customer add extra parameters to the payload
            .setInAppPurchaseEventDataSource {
                mapOf(
                    "some key" to "some value", "another key" to it.size
                )
            }
            // Subscriptions Purchase Validation listener. Invoked after getting response from AppsFlyer servers
            // to let customer know if purchase was validated successfully
            .setSubscriptionValidationResultListener(object :
                PurchaseClient.SubscriptionPurchaseValidationResultListener {
                override fun onResponse(result: Map<String, SubscriptionValidationResult>?) {
                    result?.forEach { (k: String, v: SubscriptionValidationResult?) ->
                        if (v.success) {
                            Log.d(
                                "dsk8",
                                "[PurchaseConnector]: Subscription with ID $k was validated successfully"
                            )
                            val subscriptionPurchase = v.subscriptionPurchase
                            Log.d("dsk8", subscriptionPurchase.toString())
                        } else {
                            Log.d(
                                "dsk8",
                                "[PurchaseConnector]: Subscription with ID $k wasn't validated successfully"
                            )
                            val failureData = v.failureData
                            Log.d("dsk8", failureData.toString())
                        }
                    }
                }

                override fun onFailure(result: String, error: Throwable?) {
                    Log.d("dsk8", "[PurchaseConnector]: Validation fail: $result")
                    error?.printStackTrace()
                }
            })
            // In Apps Purchase Validation listener. Invoked after getting response from AppsFlyer servers
            // to let customer know if purchase was validated successfully
            .setInAppValidationResultListener(object :
                PurchaseClient.InAppPurchaseValidationResultListener {
                override fun onResponse(result: Map<String, InAppPurchaseValidationResult>?) {
                    result?.forEach { (k: String, v: InAppPurchaseValidationResult?) ->
                        if (v.success) {
                            Log.d(
                                "dsk8",
                                "[PurchaseConnector]:  Product with Purchase Token$k was validated successfully"
                            )
                            val productPurchase = v.productPurchase
                            Log.d("dsk8", productPurchase.toString())
                        } else {
                            Log.d(
                                "dsk8",
                                "[PurchaseConnector]:  Product with Purchase Token $k wasn't validated successfully"
                            )
                            val failureData = v.failureData
                            Log.d(TAG, failureData.toString())
                        }
                    }
                }

                override fun onFailure(result: String, error: Throwable?) {
                    Log.d("dsk8", "[PurchaseConnector]: Validation fail: $result")
                    error?.printStackTrace()
                }
            })
            // Build the client
            .build()

        // Start the SDK instance to observe transactions.
        afPurchaseClient.startObservingTransactions()


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
        readDataJson()
    }

    private fun readDataJson() {
        try {
            val data = Utils.getStringAssetFile(
                pathJson, application
            )
            val ads = if (getJsonCache().exists()) {
                try {
                    val fileReader = FileReader(getJsonCache())
                    val json = gson.fromJson(fileReader, Ads::class.java)
                    fileReader.close()
                    json
                } catch (e: Exception) {
                    gson.fromJson(data, Ads::class.java)
                }
            } else {
                gson.fromJson(data, Ads::class.java)
            }
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

    private fun getJsonCache(): File {
        return File(application.filesDir, "ads_cache.json")
    }

    fun setAdData(adJson: String) {
        kotlin.runCatching {
            if (adJson.isNotEmpty()) {
                getJsonCache().writeText(adJson)
            }
        }.onFailure {
            it.printStackTrace()
        }
        kotlin.runCatching {
            val ads = gson.fromJson(adJson, Ads::class.java)
            if (checkAppIdPacket(ads)) {
                hashMapAds.clear()
                ads.listAdsChild.forEach {
                    if (it.adsType.lowercase() == AdDef.ADS_TYPE.OPEN_APP_RESUME) {
                        adsOpenResume = it
                    }
                    hashMapAds[it.spaceName] = it
                }
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun isAllowedShow(spaceName: String): Boolean {
        return !isPremium && hashMapAds[spaceName]?.status == true
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

    fun clearAllAd() {
        admobHolder.clearAllAd()
    }

    fun preloadAdsResume() {
        if (!isPremium) {
            activity?.let {
                val runnable = Runnable {
                    isAutoShowAdsResume = true
                    adsOpenResume?.let { adsChild ->
                        if (!adsChild.status) {
                            return@Runnable
                        }

                        val status = admobHolder.getStatusPreload(adsChild)
                        if (status != StateLoadAd.LOADING && status != StateLoadAd.SUCCESS) {
                            admobHolder.preload(it, adsChild)
                        }
                    }
                }
                if (isWaitCMP) {
                    listRunnable.add(runnable)
                } else {
                    runnable.run()
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
                Handler(Looper.getMainLooper()).postDelayed({
                    isShowAdsFullScreen = false
                }, 1000)
            }

            override fun onAdFailToLoad(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(adsChild, messageError)
                showToastDebug(
                    "Fail to load ${adsChild.adsType} ${adsChild.spaceName}", adsChild.adsIds
                )
            }

            override fun onAdFailToShow(messageError: String?) {
                adCallback?.onAdFailToLoad(messageError)
                adCallbackAll?.onAdFailToLoad(adsChild, messageError)
                isShowAdsFullScreen = false
                showToastDebug(
                    "Fail to show ${adsChild.adsType} ${adsChild.spaceName}", adsChild.adsIds
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
                val runnable = Runnable {
                    hashMapAds[spaceName]?.let { ads ->
                        if (!ads.status) {
                            preloadCallback?.onLoadFail()
                            return@Runnable
                        }
                        if (ads.adsType == INTERSTITIAL || ads.adsType == AdDef.ADS_TYPE.OPEN_APP || ads.adsType == AdDef.ADS_TYPE.REWARD_VIDEO) {
                            showToastDebug("Pre load ${ads.adsType} ${ads.spaceName}", ads.adsIds)
                        }
                        admobHolder.preload(it, ads, preloadCallback)
                    }
                }
                if (isWaitCMP) {
                    listRunnable.add(runnable)
                } else {
                    runnable.run()
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
                val runnable = Runnable {
                    hashMapAds[spaceName]?.let { ads ->
                        if (!ads.status) {
                            adCallback?.onAdFailToLoad(ERROR_AD_OFF)
                            return@Runnable
                        }
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
                if (isWaitCMP) {
                    listRunnable.add(runnable)
                } else {
                    runnable.run()
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
            val runnable = Runnable {
                activity?.let {
                    hashMapAds[spaceName]?.let { ads ->
                        if (!ads.status) {
                            adCallback?.onAdFailToLoad(Constant.ERROR_AD_OFF)
                            return@Runnable
                        }

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
            if (isWaitCMP) {
                listRunnable.add(runnable)
            } else {
                runnable.run()
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
            val runnable = Runnable {
                activity?.let {
                    hashMapAds[spaceName]?.let { ads ->
                        if (!ads.status) {
                            adCallback?.onAdFailToLoad(ERROR_AD_OFF)
                            return@Runnable
                        }

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
            if (isWaitCMP) {
                listRunnable.add(runnable)
            } else {
                runnable.run()
            }
        } else {
            adCallback?.onAdFailToLoad("premium")
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

    fun getAdView(spaceName: String): AdView? {
        val item = hashMapAds[spaceName]
        if (item != null) {
            return admobHolder.getAdView(item)
        }
        return null
    }


    fun destroy(spaceName: String) {
        val adsChild = hashMapAds[spaceName]
        adsChild?.let { admobHolder.destroy(it) }
    }

    fun destroyAll() {
        hashMapAds.values.forEach {
            admobHolder.destroy(it)
        }
    }

    fun cmpComplete() {
        isWaitCMP = false
        listRunnable.forEach {
            it.run()
        }
        listRunnable.clear()
    }
}
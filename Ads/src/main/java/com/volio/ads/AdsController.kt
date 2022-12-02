package com.volio.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.volio.ads.admob.AdmobHolder
import com.volio.ads.fan.AudienceNetworkInitializeHelper
import com.volio.ads.fan.FanHolderHolder
import com.volio.ads.model.Ads
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.*
import com.volio.ads.utils.Utils.showToastDebug
import java.util.*

private const val TAG = "AdsController"

class AdsController private constructor(
    var activity: Activity,
    var listAppId: ArrayList<String>,
    var packetName: String,
    var listPathJson: ArrayList<String>,
    var lifecycleActivity: Lifecycle
) {
    private var gson = Gson()
    private val hashMapAds: HashMap<String, ArrayList<AdsChild>> = hashMapOf()
    private val admobHolder = AdmobHolder()
    private val fanHolder = FanHolderHolder()
    private var connectionLiveData: ConnectionLiveData = ConnectionLiveData(activity)
    private var isConnection: Boolean = true
    var isPremium: Boolean = false

    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var adsController: AdsController

        @SuppressLint("StaticFieldLeak")
        var mTopActivity : Activity? = null

        fun init(
            activity: Activity,
            isDebug: Boolean,
            listAppId: ArrayList<String>,
            packetName: String,
            listPathJson: ArrayList<String>, lifecycle: Lifecycle
        ) {
            Constant.isDebug = isDebug
            adsController = AdsController(activity, listAppId, packetName, listPathJson, lifecycle)

            activity.application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

                }

                override fun onActivityStarted(activity: Activity) {

                }

                override fun onActivityResumed(activity: Activity) {
                    mTopActivity = activity
                }

                override fun onActivityPaused(activity: Activity) {

                }

                override fun onActivityStopped(activity: Activity) {

                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

                }

                override fun onActivityDestroyed(activity: Activity) {
                    mTopActivity = null
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

    public fun setDebugMode(isDebug: Boolean) {
        Constant.isDebug = isDebug
    }

    public fun getDebugMode() = Constant.isDebug
    private fun checkAppIdPacket(ads: Ads): Boolean {
        var checkAppId = false
        var checkPacket = false
        for (id in listAppId) {
            if (ads.appId == id) {
                checkAppId = true
                break
            }
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
        connectionLiveData.observe({ lifecycleActivity }, {
            isConnection = it
            Log.d(TAG, "isConnect: $isConnection")
        })
        AudienceNetworkInitializeHelper.initialize(activity)
        val listAds = ArrayList<Ads>()
        for (item in listPathJson) {
            try {
                val data = Utils.getStringAssetFile(item, activity)
                Log.d(TAG, ": $data")
                val ads = gson.fromJson<Ads>(data, Ads::class.java)

                listAds.add(ads)
            } catch (e: Exception) {
                showToastDebug(activity, "no load data json ads file")
            }
        }
        for (ads in listAds) {
            for (adsChild in ads.listAdsChild) {
                if (!checkAppIdPacket(ads)) continue
                adsChild.network = ads.network
                adsChild.adsId = adsChild.adsId.trim()
                if (adsChild.priority == -1) adsChild.priority = ads.priority
                var listItem = hashMapAds[adsChild.spaceName.toLowerCase(Locale.getDefault())]
                if (listItem == null) {
                    listItem = ArrayList()
                    hashMapAds[adsChild.spaceName.toLowerCase(Locale.getDefault())] = listItem
                }
                listItem.add(adsChild)
                Log.d(TAG, ": ${adsChild.toString()}")
            }
        }

    }


    public fun preload(spaceName: String, preloadCallback: PreloadCallback? = null) {
        if (isPremium) return
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null && listItem.size > 0) {
            for (item in listItem) {
                when (item.network.toLowerCase(Locale.getDefault())) {
                    AdDef.NETWORK.GOOGLE -> {
                        admobHolder.preload(activity, item, preloadCallback)
                    }
                    AdDef.NETWORK.FACEBOOK -> {
                        fanHolder.preload(activity, item)
                    }
                    else -> {
                        showToastDebug(
                            activity,
                            "not support network ${item.network} check file json"
                        )
                    }
                }
            }
        } else {
            showToastDebug(activity, "no data check spaceName and file json")
        }
    }


    public fun checkAD(spaceName: String): StateLoadAd {
        if (isPremium) {
            return StateLoadAd.NONE
        }
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null && listItem.size > 0) {
            var s = StateLoadAd.NONE
            for (item in listItem) {
                when (item.network.toLowerCase(Locale.getDefault())) {
                    AdDef.NETWORK.GOOGLE -> {
                        admobHolder.checkStateAD(activity, item, object : StateADCallback {
                            override fun onState(state: StateLoadAd) {
                                s = state
                            }
                        })
                    }
//                    AdDef.NETWORK.FACEBOOK ->{
//                        fanHolder.preload(activity,item)
//                    }
                    else -> {
                        showToastDebug(
                            activity,
                            "not support network ${item.network} check file json"
                        )
                    }

                }
            }
            return s
        } else {
            showToastDebug(activity, "no data check spaceName and file json")
            return StateLoadAd.NONE
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
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null) {
            for (item in listItem) {
                if (item.network.toLowerCase(Locale.getDefault()) == AdDef.NETWORK.GOOGLE) {
                    admobHolder.showLoadedAd(
                        activity,
                        item,
                        loadingText,
                        layout,
                        layoutAds,
                        lifecycle,
                        timeMillisecond,
                        adCallback
                    )
                    break
                }
            }
        }
    }


    public fun show(
        spaceName: String,
        reloadLoadSpaceName: String? = null,
        textLoading: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long = Constant.TIME_OUT_DEFAULT,
        timeDelayShowAd: Int = 0,
        adCallback: AdCallback? = null
    ) {
        if (isPremium) {
            layout?.visibility = View.GONE
            adCallback?.onAdShow("", "")
            return
        }
        if (!isConnection) {
            adCallback?.onAdFailToLoad(Constant.ERROR_NO_INTERNET)
            return
        }
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null && listItem.size > 0) {
            listItem.sortWith(compareBy { it.priority })
            var checkShow = false
            for (item in listItem) {
                when (item.network.toLowerCase(Locale.getDefault())) {
                    AdDef.NETWORK.GOOGLE -> {
                        checkShow = admobHolder.show(
                            activity,
                            item,
                            textLoading,
                            layout,
                            layoutAds,
                            timeDelayShowAd, lifecycle,
                            adCallback
                        )
                    }
                    AdDef.NETWORK.FACEBOOK -> {
                        checkShow = fanHolder.show(
                            activity,
                            item,
                            textLoading,
                            layout,
                            layoutAds,
                            adCallback
                        )
                    }
                }
                if (checkShow) break
            }
            if (!checkShow && reloadLoadSpaceName != null) {
                loadAndShow(
                    reloadLoadSpaceName,
                    false,
                    textLoading,
                    layout,
                    layoutAds,
                    lifecycle,
                    timeMillisecond,
                    adCallback
                )
            }
        } else {
            showToastDebug(activity, "no data check spaceName and file json")
            adCallback?.onAdFailToLoad("")

        }
    }

    public fun loadAndShow(
        spaceName: String,
        isKeepAds: Boolean = false,
        loadingText: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long? = null,
        adCallback: AdCallback? = null
    ) {
        if (isPremium) {
            layout?.visibility = View.GONE
            adCallback?.onAdShow("", "")
            return
        }
        if (!isConnection) {
            adCallback?.onAdFailToLoad(Constant.ERROR_NO_INTERNET)
            return
        }
        val contextUse = this.activity
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem == null || listItem.size == 0) {
            showToastDebug(contextUse, "no data check spaceName or file json")
            adCallback?.onAdFailToLoad("")
        } else {
            val adsChild = getChildPriority(listItem, -1)
            Log.d(TAG, "loadAndShow: ${adsChild?.toString()}")
            if (adsChild != null) {
                loadAdsPriority(
                    contextUse,
                    isKeepAds,
                    adsChild,
                    loadingText,
                    layout,
                    layoutAds,
                    lifecycle,
                    timeMillisecond,
                    listItem,
                    adCallback
                )
            } else {
                showToastDebug(contextUse, "no data check priority file json")
                adCallback?.onAdFailToLoad("")

            }

        }

    }

    private fun loadAdsPriority(
        context: Context,
        isKeepAds: Boolean,
        adsChild: AdsChild,
        loadingText: String?,
        layout: ViewGroup?,
        layoutAds: View?,
        lifecycle: Lifecycle?,
        timeMillisecond: Long?,
        listItem: ArrayList<AdsChild>,
        adCallback: AdCallback?
    ) {
        Log.d(TAG, "loadAdsPriority: $adsChild")
        when (adsChild.network.toLowerCase(Locale.getDefault())) {
            AdDef.NETWORK.GOOGLE -> {
                Log.d(TAG, "loadAdsPriority: 1")
                admobHolder.loadAndShow(activity,
                    isKeepAds,
                    adsChild,
                    loadingText,
                    layout,
                    layoutAds,
                    lifecycle,
                    timeMillisecond,
                    adCallback,
                    failCheck = {
                        val item = getChildPriority(listItem, adsChild.priority)
                        return@loadAndShow if (item == null) {
                            true
                        } else {
                            loadAdsPriority(
                                context,
                                isKeepAds,
                                item,
                                loadingText,
                                layout,
                                layoutAds,
                                lifecycle,
                                timeMillisecond,
                                listItem,
                                adCallback
                            )
                            false
                        }
                    }
                )

            }
            AdDef.NETWORK.FACEBOOK -> {
                Log.d(TAG, "loadAdsPriority: 2")
                fanHolder.loadAndShow(activity,
                    isKeepAds,
                    adsChild,
                    loadingText,
                    layout,
                    layoutAds,
                    lifecycle,
                    timeMillisecond,
                    adCallback,
                    failCheck = {
                        Log.d(TAG, "loadAdsPriority3: ${adsChild.priority}")
                        val item = getChildPriority(listItem, adsChild.priority)
                        Log.d(TAG, "loadAdsPriority4: ${item?.priority}")

                        return@loadAndShow if (item == null) {
                            true
                        } else {
                            loadAdsPriority(
                                context,
                                isKeepAds,
                                item,
                                loadingText,
                                layout,
                                layoutAds,
                                lifecycle,
                                timeMillisecond,
                                listItem,
                                adCallback
                            )
                            false
                        }
                    }
                )

            }
            else -> {
                showToastDebug(context, "not support network ${adsChild.network} check file json")
                adCallback?.onAdFailToLoad("")
            }
        }
    }


    private fun getChildPriority(listItem: ArrayList<AdsChild>, priority: Int): AdsChild? {
        var value = Int.MAX_VALUE
        var adsChild: AdsChild? = null
        for (item in listItem) {
            if (item.priority > priority) {
                if (item.priority < value) {
                    value = item.priority
                    adsChild = item
                }
            }
        }
        return adsChild
    }

    public fun allowShowAds(allow: Boolean, spaceName: String, network: String) {
        if (allow) return
        val list = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        Log.d(TAG, "allowShowAds: 1")
        list?.let {
            Log.d(TAG, "allowShowAds: 2")
            for (item in it) {
                if (item.network == network.toLowerCase(Locale.getDefault())) {
                    it.remove(item)
                    break
                }
            }
            hashMapAds[spaceName] = it
        }
    }

    fun getStatusPreload(spaceName: String): StateLoadAd {
//        if (!isConnection) return StateLoadAd.NO_INTERNET
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null) {
            for (item in listItem) {
                if (item.network.toLowerCase(Locale.getDefault()) == AdDef.NETWORK.GOOGLE) {
                    return admobHolder.getStatusPreload(item)
                }
            }
        }
        return StateLoadAd.NONE
    }

    public fun setPriority(spaceName: String, network: String) {
        val list = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        list?.let {
            var min = 0
            for (item in list) {
                if (item.network == network.toLowerCase(Locale.getDefault())) {
                    item.priority = 0
                } else {
                    min += 1
                    item.priority = min
                }
            }
        }
    }

    fun destroy(spaceName: String) {
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        listItem?.let {
            for (item in it) {
                admobHolder.destroy(item)
            }
        }
    }

}
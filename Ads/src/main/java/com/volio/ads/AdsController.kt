package com.volio.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.volio.ads.admob.AdmobHolder
import com.volio.ads.model.Ads
import com.volio.ads.model.AdsChild
import com.volio.ads.utils.AdDef
import com.volio.ads.utils.Constant
import com.volio.ads.utils.Utils
import com.volio.ads.utils.Utils.showToastDebug
import m.tech.duonglieulibrary.util.ConnectionLiveData
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

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
    private var connectionLiveData : ConnectionLiveData = ConnectionLiveData(activity)
    private var isConnection:Boolean = true
    var isPremium:Boolean = false
    companion object {
        private lateinit var adsController: AdsController
        fun init(
            activity: Activity,
            isDebug:Boolean,
            listAppId: ArrayList<String>,
            packetName: String,
            listPathJson: ArrayList<String>, lifecycle: Lifecycle
        ) {
            Constant.isDebug = isDebug
            adsController = AdsController(activity, listAppId, packetName, listPathJson,lifecycle)
        }

        fun getInstance(): AdsController {
            if (!::adsController.isInitialized) {
                throw Throwable("call init")
            }
            return adsController
        }
    }
    public fun setDebugMode(isDebug: Boolean){
        Constant.isDebug = isDebug
    }
    private fun checkAppIdPacket(ads:Ads):Boolean{
        var checkAppId = false
        var checkPacket = false
        for (id in listAppId){
            if (ads.appId == id) {
                checkAppId = true
                break
            }
        }
        if(!checkAppId) showToastDebug(activity, "wrong appId network ${ads.network}")

        if (ads.packetName != packetName) {
            showToastDebug(activity, "wrong packetName")
        }else{
            checkPacket = true
        }
        return checkAppId && checkPacket
    }
    init {
        connectionLiveData.observe({ lifecycleActivity }, {
            isConnection = it
            Log.d(TAG, "isConnect: $isConnection")
        })
        val listAds = ArrayList<Ads>()
        for (item in listPathJson) {
            try {
                val data = Utils.getStringAssetFile(item,activity)
                Log.d(TAG, ": $data")
                val ads = gson.fromJson<Ads>(data, Ads::class.java)

                listAds.add(ads)
            } catch (e: Exception) {
                showToastDebug(activity, "no load data json ads file")
            }
        }
        for (ads in listAds) {
            for (adsChild in ads.listAdsChild) {
                if(!checkAppIdPacket(ads)) continue
                adsChild.network = ads.network
                if(adsChild.priority == -1) adsChild.priority = ads.priority
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



    public fun preload(spaceName: String) {
        if (isPremium) return
        val listItem = hashMapAds[spaceName.toLowerCase(Locale.getDefault())]
        if (listItem != null && listItem.size > 0) {
            for (item in listItem) {
                when (item.network.toLowerCase(Locale.getDefault())) {
                    AdDef.NETWORK.GOOGLE -> {
                        admobHolder.preload(activity, item)
                    }
                    else ->{
                        showToastDebug(activity, "not support network ${item.network} check file json")
                    }
                }
            }
        } else {
            showToastDebug(activity, "no data check spaceName and file json")
        }

    }
    public fun show(
        spaceName: String,
        reloadLoadSpaceName:String? = null,
        textLoading: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long = Constant.TIME_OUT_DEFAULT,
        adCallback: AdCallback? =null
    ) {
        if(isPremium){
            adCallback?.onAdShow("","")
        }
        if (!isConnection){
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
                            adCallback
                        )
                    }
                }
                if (checkShow) break
            }
            if (!checkShow) {
                if(reloadLoadSpaceName != null) {
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
                }else{
                    adCallback?.onAdFailToLoad("")
                }
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
        if(isPremium){
            adCallback?.onAdShow("","")
        }
        if (!isConnection){
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
        Log.d(TAG, "loadAdsPriority: 1")
        when (adsChild.network.toLowerCase(Locale.getDefault())) {
            AdDef.NETWORK.GOOGLE -> {
                Log.d(TAG, "loadAdsPriority: 2")
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
                                adsChild,
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

}
package com.volio.ads.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import com.appsflyer.adrevenue.adnetworks.generic.Scheme
import com.volio.ads.AdsController
import com.volio.ads.AdsController.Companion.activity
import java.util.*
import kotlin.collections.HashMap

object AppFlyerUtils {


    var keyAppFlyer = "4Ti9yuyaVb6BJMoy25gWUP"
    var isEnableTiktokEvent = false

    fun logAdRevenue(bundle: Bundle) {
        if (!AdsController.checkInit()) return
        val value = bundle.getString("revenue_micros", "0").toInt() / 1000000.0
        if (isEnableTiktokEvent){
            activity?.let{
                AppsFlyerLib.getInstance().logEvent(
                    it, "ad_roas_tiktok", mapOf<String, Double>("af_revenue" to value)
                )
            }
        }
        //ad source name
        val customParams: MutableMap<String, String> = HashMap()
        customParams[Scheme.AD_UNIT] = bundle.getString("ad_unit_id", "")
        AppsFlyerAdRevenue.logAdRevenue(
            bundle.getString("ad_source_name", ""),
            MediationNetwork.googleadmob,
            Currency.getInstance(Locale.US),
            value, customParams
        )
    }
}
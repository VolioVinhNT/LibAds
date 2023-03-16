package com.volio.ads

import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.volio.ads.utils.Constant

interface AdCallback {

    fun onAdShow(network: String, adtype: String)
    fun onAdClose(adType: String)
    fun onAdFailToLoad(messageError: String?)
    fun onAdFailToShow(messageError: String?) {}
    fun onAdOff()
    fun onAdClick() {}
    fun onPaidEvent(params: Bundle) {
        if (Constant.isTrackAdRevenue) {
            val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
            val amount = params.getString("valuemicros")?.toInt()
            val currencyCode = params.getString("currency")
            val finalRevenue: Double = amount!! / 1000000.0
            Log.d("dsk8", "${params.getString("adunitid")} - $finalRevenue : $currencyCode")
            adRevenue.setRevenue(finalRevenue, currencyCode)
            Adjust.trackAdRevenue(adRevenue)
        }
    }

    fun onRewardShow(network: String, adtype: String) {}
    fun onAdImpression(adType: String) {

    }
}
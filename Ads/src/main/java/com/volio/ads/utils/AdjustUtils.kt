package com.volio.ads.utils

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent

object AdjustUtils {


    var isLogAdImpression = false
    var isLogAdClick = false


    var ad_impression = "ad_impression"
    var ad_click = "ad_click"


    var isTrackAdRevenue = false

    fun logEvent(name: String) {
        Adjust.trackEvent(AdjustEvent(name))
    }

}
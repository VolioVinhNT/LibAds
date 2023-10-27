package com.volio.ads

import android.os.Bundle
import com.volio.ads.model.AdsChild

interface AdCallbackAll {

    fun onAdShow(adsChild: AdsChild) {}
    fun onAdClose(adsChild: AdsChild) {}
    fun onAdFailToLoad(adsChild: AdsChild, messageError: String?) {}
    fun onAdFailToShow(adsChild: AdsChild, messageError: String?) {}
    fun onAdOff(adsChild: AdsChild) {}
    fun onAdClick(adsChild: AdsChild) {}
    fun onPaidEvent(adsChild: AdsChild, params: Bundle) {}
    fun onRewardShow(adsChild: AdsChild) {}
    fun onAdImpression(adsChild: AdsChild) {}
}
package com.volio.ads

import android.os.Bundle

interface AdImpressionCallback : AdCallback {
    override fun onAdShow(network: String, adtype: String){}
    override fun onAdClose(adType: String){}
    override fun onAdFailToLoad(messageError: String?){}
    override fun onAdOff(){}
    override fun onAdClick(){}
    override fun onPaidEvent(params: Bundle) {}
    override fun onRewardShow(network: String, adtype: String) {}
    override fun onAdImpression(adType: String)
}
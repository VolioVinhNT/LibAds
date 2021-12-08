package com.volio.ads

import android.os.Bundle
import com.volio.ads.utils.AdDef

interface AdCallback {

    fun onAdShow(network: String, adtype: String)
    fun onAdClose(adType: String)
    fun onAdFailToLoad(messageError: String?)
    fun onAdOff()
    fun onAdClick(){}
    fun onPaidEvent(params: Bundle) {}

}
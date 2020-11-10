package com.volio.ads

import com.volio.ads.utils.AdDef

interface AdCallback {

    fun onAdShow(network: String, adtype: String)
    fun onAdClose(adType: String)
    fun onAdFailToLoad(messageError: String?)
    fun onAdOff()

}
package com.volio.ads

import com.volio.ads.utils.StateLoadAd

interface StateADCallback {
    fun onState(state: StateLoadAd)
}
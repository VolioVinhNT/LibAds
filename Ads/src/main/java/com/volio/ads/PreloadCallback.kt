package com.volio.ads

interface PreloadCallback {
    fun onLoadDone()
    fun onLoadFail(){}
}
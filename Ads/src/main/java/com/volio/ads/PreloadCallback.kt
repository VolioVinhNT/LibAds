package com.volio.ads

fun interface PreloadCallback {
    fun onLoadDone()
    fun onLoadFail(){}
}
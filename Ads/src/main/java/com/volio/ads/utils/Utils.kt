package com.volio.ads.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringDef
import java.io.InputStream

object Utils {
    fun showToastDebug(context: Context?,text:String){
        if(context != null&&Constant.isDebug) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
    fun getStringAssetFile(path: String,activity:Activity): String? {
        var json: String? = null
        try {
            val inputStream: InputStream = activity.assets.open(path)
            json = inputStream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return json
    }


}
package com.volio.ads.model

import com.google.gson.annotations.SerializedName
import com.volio.ads.utils.AdDef

class AdsChild {
    var network:String = "google"
    @SerializedName("spaceName")
    var spaceName:String = "null"
    @SerializedName("adsType")
    var adsType:String = "null"
    @SerializedName("id")
    var adsId:String = "null"
    @SerializedName("priority")
    var priority:Int = -1
    var adsSize:String = AdDef.GOOGLE_AD_BANNER.BANNER_320x50
    override fun toString(): String {
        return "AdsChild(network='$network', spaceName='$spaceName', adsType='$adsType', adsId='$adsId', priority=$priority)"
    }


}
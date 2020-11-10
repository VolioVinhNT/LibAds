package com.volio.ads.model

import com.google.gson.annotations.SerializedName

class Ads {
    @SerializedName("network")
    var network:String = "google"
    @SerializedName("priority")
    var priority:Int = 0
    @SerializedName("appId")
    var appId:String = ""
    @SerializedName("packetName")
    var packetName:String = ""
    @SerializedName("listAds")
    var listAdsChild:ArrayList<AdsChild> = ArrayList()
    override fun toString(): String {
        return "Ads(appId='$appId', packetName='$packetName', listAdsChild=$listAdsChild)"
    }

}
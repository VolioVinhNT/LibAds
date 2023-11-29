package com.volio.ads.model

import com.google.gson.annotations.SerializedName
import com.volio.ads.utils.AdDef

class AdsChild {
    var spaceName:String = "null"

    @SerializedName("status")
    var status : Boolean = true

    @SerializedName("adsType")
    var adsType:String = "null"
    @SerializedName("adsIds")
    var adsIds:List<AdsId> = listOf()
    private var currentIndexAds:Int = 0

    var adsSize:String = AdDef.GOOGLE_AD_BANNER.MEDIUM_RECTANGLE_300x250
    override fun toString(): String {
        return "AdsChild(spaceName='$spaceName', adsType='$adsType', adsId='$adsIds', priority=$currentIndexAds),status= $status"
    }


}
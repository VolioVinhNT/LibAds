package com.volio.ads.model

import com.google.gson.annotations.SerializedName

class AdsId {
    @SerializedName("priority")
    var priority:Int = 0
    @SerializedName("id")
    var id:String = ""
    override fun toString(): String {
        return "{priority: $priority ,id: $id }"
    }
}
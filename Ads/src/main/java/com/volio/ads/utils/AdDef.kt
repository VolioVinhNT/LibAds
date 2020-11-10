package com.volio.ads.utils


class AdDef {
    class NETWORK {
        companion object {
            const val GOOGLE = "google"
            const val FACEBOOK = "facebook"
        }
    }

    class ADS_TYPE {
        companion object {
            const val INTERSTITIAL = "interstitial"
            const val NATIVE = "native"
            var BANNER = "banner"
            var BANNER_ADAPTIVE = "banner_adaptive"
            var REWARD_VIDEO = "reward_video"
            var OPEN_APP =  "open_app"
        }
    }
     class GOOGLE_AD_BANNER {
        companion object {
            const val BANNER_320x50 = "BANNER_320x50"
            const val LARGE_BANNER_320x100 = "LARGE_BANNER_320x100 "
            const val MEDIUM_RECTANGLE_300x250 = "MEDIUM_RECTANGLE_300x250"
            const val FULL_BANNER_468x60 = "FULL_BANNER_468x60"
            const val LEADERBOARD_728x90 = "LEADERBOARD_728x90"
            const val SMART_BANNER = "SMART_BANNER "
        }
    }
}
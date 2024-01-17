package com.volio.cmp

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.volio.ads.AdsController

class CMPController constructor(private var activity: Activity) {

    fun isGDPR(): Boolean {
        return GDPRUtils.isGDPR(activity)
    }

    fun showCMP(isTesting: Boolean, titleDialog: String? = null, desDialog: String? = null) {
        val dialog = DialogLoadingForm(activity, titleDialog, desDialog)
        dialog.show()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        if (consentInformation.isConsentFormAvailable) {
            UserMessagingPlatform.loadConsentForm(activity,
                {
                    dialog.dismiss()
                    it.show(activity) {
                    }
                }, {
                    dialog.dismiss()
                })

        } else {
            showCMP(isTesting, cmpCallback = object : CMPCallback {
                override fun onShowAd() {
                    dialog.dismiss()
                }

                override fun onChangeScreen() {
                    dialog.dismiss()
                }
            })
        }

    }

    fun showCMP(isTesting: Boolean, cmpCallback: CMPCallback) {
        val codeGDPR = GDPRUtils.isGDPR2(activity)
        if (codeGDPR == 0) {
            cmpCallback.onShowAd()
            AdsController.getInstance().cmpComplete()
        } else {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(Utils.getDeviceID(activity)).build()

            val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
                .setConsentDebugSettings(if (isTesting) debugSettings else null).build()

            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            consentInformation.requestConsentInfoUpdate(activity, params, {
                Log.d("dsk6", "a: ")
                val canRequestAds = GDPRUtils.canShowAds(activity)
                val canrequest = consentInformation.canRequestAds()
                if (canRequestAds) {
                    Log.d("dsk6", "c: ")
                    GDPRUtils.isUserConsent = true
                    cmpCallback.onShowAd()
                    AdsController.getInstance().cmpComplete()
                } else {
                    Log.d("dsk6", "d: ")
                    UserMessagingPlatform.loadConsentForm(activity, {
                        it.show(activity) {
                            val canRequestAds = GDPRUtils.canShowAds(activity)
                            if (canRequestAds) {
                                GDPRUtils.isUserConsent = true
                                cmpCallback.onShowAd()
                                AdsController.getInstance().cmpComplete()
//                            showAd()
                            } else {
                                GDPRUtils.isUserConsent = false
                                cmpCallback.onChangeScreen()
                                AdsController.getInstance().cmpComplete()
//                            changeScreen()
                            }
                        }
                    }, {
                        Log.d("dsk6", "e: ${it.message}")
                        cmpCallback.onShowAd()
                        AdsController.getInstance().cmpComplete()
                    })
                }
            }, { requestConsentError ->
                Log.d("dsk6", "b: ${requestConsentError.message}")
                cmpCallback.onChangeScreen()
                AdsController.getInstance().cmpComplete()
            })
        }
    }


    companion object {

//        @SuppressLint("StaticFieldLeak")
//        private lateinit var cmpController: CMPController
//
//
//        fun init(
//            activity: Activity,
//        ) {
//            cmpController = CMPController(activity)
//        }
//
//
//        fun getInstance(): CMPController {
//            if (!::cmpController.isInitialized) {
//                throw Throwable("call init")
//            }
//            return cmpController
//        }
//
//        fun checkInit() = ::cmpController.isInitialized
    }
}
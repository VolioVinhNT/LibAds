package com.volio.cmp

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.volio.ads.AdsController
import com.volio.cmp.GDPRUtils.canShowAds

class CMPController constructor(private var activity: Activity) {

    fun isGDPR(): Boolean {
        return GDPRUtils.isGDPR(activity)
    }

    var isUserConsent = true
        private set

    val canShowAd: Boolean
        get() = canShowAds(activity)

    fun showCMP(activity: AppCompatActivity, isTesting: Boolean = false, onDone: () -> Unit) {

//        if (!isGDPR()) {
//            isUserConsent = true
//            onDone.invoke()
//            return
//        }

        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(Utils.getDeviceID(activity)).build()

        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(if (isTesting) debugSettings else null).build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(activity, params, {

            val canRequestAds = canShowAds(activity)

            if (canRequestAds) {
                isUserConsent = true
                onDone.invoke()
            } else {
                UserMessagingPlatform.loadConsentForm(activity, {
                    it.show(activity) {
                        if (canShowAd) {
                            isUserConsent = true
                            onDone.invoke()
                        } else {
                            isUserConsent = false
                            onDone.invoke()
                        }
                    }
                }, {
                    onDone.invoke()
                })
            }
        }, { _ ->
            onDone.invoke()
        })
    }

    fun showCMP(activity: AppCompatActivity, isTesting: Boolean) {
        val loading = DialogLoadingForm(activity)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                if (loading.isShowing) {
                    loading.dismiss()
                }
            }
        })

        loading.show()

        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(Utils.getDeviceID(activity)).build()

        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
            .setConsentDebugSettings(if (isTesting) debugSettings else null).build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(activity, params, {

            UserMessagingPlatform.loadConsentForm(activity, {
                it.show(activity) {
                    if (canShowAd) {
                        isUserConsent = true
                        if (activity.lifecycle.currentState == Lifecycle.State.RESUMED && loading.isShowing) {
                            loading.dismiss()
                        }
                    } else {
                        isUserConsent = false
                        if (activity.lifecycle.currentState == Lifecycle.State.RESUMED && loading.isShowing) {
                            loading.dismiss()
                        }
                    }
                }
            }, {
                if (activity.lifecycle.currentState == Lifecycle.State.RESUMED && loading.isShowing) {
                    loading.dismiss()
                }
            })
        }, { _ ->
            if (activity.lifecycle.currentState == Lifecycle.State.RESUMED && loading.isShowing) {
                loading.dismiss()
            }
        })

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
                    isUserConsent = true
                    cmpCallback.onShowAd()
                    AdsController.getInstance().cmpComplete()
                } else {
                    Log.d("dsk6", "d: ")
                    UserMessagingPlatform.loadConsentForm(activity, {
                        it.show(activity) {
                            val canRequestAds = GDPRUtils.canShowAds(activity)
                            if (canRequestAds) {
                                isUserConsent = true
                                cmpCallback.onShowAd()
                                AdsController.getInstance().cmpComplete()
//                            showAd()
                            } else {
                                isUserConsent = false
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
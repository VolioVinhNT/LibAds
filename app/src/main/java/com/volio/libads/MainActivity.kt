package com.volio.libads

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.volio.ads.AdsController

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        AdsController.init(
//            activity = this,
//            isDebug = true,
//            listAppId = arrayListOf("1"),
//            packetName = "com.pdfconverter.imagetopdf.jpgtopdf.pdfconverterforandroid.test",
//            listPathJson = arrayListOf("fan_id.json"),
//            lifecycle = lifecycle
//        )
//        AdsController.getInstance().loadAndShow("test",layout = findViewById(R.id.layout_ads),lifecycle = lifecycle)
        if (!AdsController.checkInit()) {
            AdsController.init(
                application = application,
                isDebug = BuildConfig.DEBUG,
                appId = "ca-app-pub-3940256099942544~3347511713",
                packetName = BuildConfig.APPLICATION_ID,
                pathJson = "fan_id.json",
            )
        }
    }
}
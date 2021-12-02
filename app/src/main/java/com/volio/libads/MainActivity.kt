package com.volio.libads

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.volio.ads.AdsController

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AdsController.init(
            activity = this,
            isDebug = true,
            listAppId = arrayListOf("1"),
            packetName = "com.pdfconverter.imagetopdf.jpgtopdf.pdfconverterforandroid.test",
            listPathJson = arrayListOf("fan_id.json"),
            lifecycle = lifecycle
        )
        AdsController.getInstance().loadAndShow("test",layout = findViewById(R.id.layout_ads),lifecycle = lifecycle)

    }
}
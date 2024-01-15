package com.volio.cmp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import com.volio.ads.R

class DialogLoadingForm(context: Context, val title: String? = null, val des: String? = null) :
    Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading_form)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        val viewRotate = findViewById<View>(R.id.imvLoading)
        title?.let {
            findViewById<TextView?>(R.id.tvTitle)?.text = it
        }
        des?.let {
            findViewById<TextView?>(R.id.tvContent)?.text = it
        }
        viewRotate.animRotation()
    }


//    override fun setContentView(layoutResID: Int) {
//        super.setContentView(layoutResID)
//        setCancelable(false)
//        setCanceledOnTouchOutside(false)
//    }

    override fun show() {
        super.show()
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


    fun View.animRotation() {
        val anim = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.interpolator = LinearInterpolator()
        anim.duration = 1500
        anim.isFillEnabled = true
        anim.repeatCount = Animation.INFINITE
        anim.fillAfter = true
        startAnimation(anim)
    }


}


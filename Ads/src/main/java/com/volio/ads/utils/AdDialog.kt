package com.volio.ads.utils

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.volio.ads.R

class AdDialog {
    companion object{
        private var mSelf: AdDialog? = null
        private var mProgressDialog: ProgressDialog? = null
        private var dialogLoading: MaterialDialog? = null
        fun getInstance(): AdDialog {
            if (mSelf == null) {
                mSelf = AdDialog()
            }
            return mSelf!!
        }

    }
    fun showLoadingWithMessage(
        context: Context?,
        message: String?
    ) {
        try {
            if (context != null&&message != null) {
                Log.i("vcvcvcvcvc","vao0")
                if (dialogLoading == null) {
                    Log.i("vcvcvcvcvc","vao1")
                    dialogLoading = MaterialDialog(context).apply {
                        cancelable(false)
                        customView(R.layout.dialog_loading_ads)
                        getCustomView().findViewById<TextView>(R.id.tvLoading).text = message

                    }
                    dialogLoading!!.show {
                        cornerRadius(10f)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    fun hideLoading() {
        try {
            if (dialogLoading != null ) {
                dialogLoading?.dismiss()
                dialogLoading = null
            }
        } catch (e: Exception) {
        }
    }
}
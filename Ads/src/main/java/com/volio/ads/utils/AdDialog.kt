package com.volio.ads.utils

import android.app.ProgressDialog
import android.content.Context

class AdDialog {
    companion object{
        private var mSelf: AdDialog? = null
        private var mProgressDialog: ProgressDialog? = null
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
        if (context != null&&message != null) {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog(context)
                mProgressDialog?.setMessage(message)
                mProgressDialog?.setCancelable(false)
                if (mProgressDialog?.window != null) {
                    mProgressDialog?.window?.setDimAmount(0f)
                }
                mProgressDialog?.show()
            }
        }
    }

    fun hideLoading() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog?.dismiss()
            mProgressDialog = null
        }
    }
}
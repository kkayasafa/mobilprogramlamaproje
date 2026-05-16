package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private var loadingDialog: android.app.Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun startActivityWithLoading(intent: Intent, finishCurrent: Boolean = false) {
        showLoading()
        Handler(Looper.getMainLooper()).postDelayed({
            hideLoading()
            startActivity(intent)
            if (finishCurrent) finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 800)
    }

    fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = android.app.Dialog(this, android.R.style.Theme_NoTitleBar_Fullscreen)
            loadingDialog?.setContentView(R.layout.dialog_loading)
            loadingDialog?.setCancelable(false)
            
            loadingDialog?.window?.let { window ->
                window.setBackgroundDrawableResource(android.R.color.transparent)
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        if (loadingDialog?.isShowing == false) {
            loadingDialog?.show()
            // show() sonrasında tekrar zorlamak bazı cihazlarda hayat kurtarır
            loadingDialog?.window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        } catch (e: Exception) { }
    }
}

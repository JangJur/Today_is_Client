package com.example.jur.today_is


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SplashActivity : Activity() {


    /**
     * Google Calendar API 호출 관련 메커니즘 및 AsyncTask을 재사용하기 위해 사용
     */


    private var mfirebaseAuth: FirebaseAuth? = null
    private var mfirebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        try {
            // 2초간 대기
            Thread.sleep(2000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mfirebaseAuth = FirebaseAuth.getInstance()
        mfirebaseUser = mfirebaseAuth!!.currentUser


        if (mfirebaseUser == null) {
            startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            finish()

        } else {
            startActivity(Intent(this@SplashActivity, CalendarActivity::class.java))
            finish()

        }


    }




}

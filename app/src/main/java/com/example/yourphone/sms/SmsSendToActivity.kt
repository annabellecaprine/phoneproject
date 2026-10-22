package com.example.yourphone.sms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.yourphone.MainActivity

class SmsSendToActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtras(this@SmsSendToActivity.intent)
        }
        startActivity(intent)
        finish()
    }
}

package com.example.yourphone.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WapPushDeliverReceiver", "MMS WAP PUSH received: ${intent.action}")
    }
}

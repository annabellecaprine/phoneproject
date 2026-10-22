package com.example.yourphone.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.yourphone.data.*
import com.example.yourphone.engine.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliverReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val firstMessage = messages[0]
        val rawNumber = firstMessage.displayOriginatingAddress ?: ""
        
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody)
        }
        val fullBody = bodyBuilder.toString()
        val timestamp = System.currentTimeMillis()

        receiverScope.launch {
            try {
                val result = RuleEngine.evaluate(
                    context = context,
                    rawNumber = rawNumber,
                    target = TargetType.SMS,
                    smsBody = fullBody
                )

                // Save log entry locally (acts as Quarantine repository)
                val database = RulesDatabase.getDatabase(context)
                database.ruleLogDao().insertLog(
                    RuleLog(
                        timestamp = timestamp,
                        rawNumber = rawNumber,
                        normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), ""),
                        actionTaken = result.action,
                        matchedRuleName = result.ruleName,
                        explainableReason = result.explainableReason,
                        targetType = TargetType.SMS,
                        messageBody = fullBody
                    )
                )

                if (result.action == RuleAction.ALLOW || result.action == RuleAction.LOG_ONLY || result.action == RuleAction.SILENCE) {
                    // ALLOW and LOG_ONLY: standard delivery
                    // SILENCE: write to inbox silently (message arrives but no active notification from this app)
                    writeToSystemInbox(context, rawNumber, fullBody, timestamp)
                } else {
                    // REJECT / ANSWER_THEN_HANG_UP: quarantine (don't write to system inbox)
                    Log.i("SmsDeliverReceiver", "SMS from $rawNumber quarantined: ${result.explainableReason}")
                }
            } catch (e: Exception) {
                Log.e("SmsDeliverReceiver", "Error processing SMS", e)
                writeToSystemInbox(context, rawNumber, fullBody, timestamp)
            }
        }
    }

    private fun writeToSystemInbox(context: Context, sender: String, body: String, timestamp: Long) {
        try {
            val values = ContentValues().apply {
                put("address", sender)
                put("body", body)
                put("date", timestamp)
                put("read", 0)
                put("type", 1)
            }
            context.contentResolver.insert(Uri.parse("content://sms/inbox"), values)
        } catch (e: Exception) {
            Log.e("SmsDeliverReceiver", "Failed to write SMS to system provider", e)
        }
    }
}

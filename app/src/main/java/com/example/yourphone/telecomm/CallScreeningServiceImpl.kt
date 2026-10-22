package com.example.yourphone.telecomm

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.yourphone.data.*
import com.example.yourphone.engine.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningServiceImpl : CallScreeningService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        
        serviceScope.launch {
            try {
                val result = RuleEngine.evaluate(
                    context = this@CallScreeningServiceImpl,
                    rawNumber = rawNumber,
                    target = TargetType.CALL
                )
                
                // Save log entry locally
                val database = RulesDatabase.getDatabase(this@CallScreeningServiceImpl)
                database.ruleLogDao().insertLog(
                    RuleLog(
                        timestamp = System.currentTimeMillis(),
                        rawNumber = rawNumber,
                        normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), ""),
                        actionTaken = result.action,
                        matchedRuleName = result.ruleName,
                        explainableReason = result.explainableReason,
                        targetType = TargetType.CALL
                    )
                )

                val responseBuilder = CallResponse.Builder()
                when (result.action) {
                    RuleAction.SILENCE -> {
                        responseBuilder.setSilenceCall(true)
                    }
                    RuleAction.REJECT -> {
                        responseBuilder.setDisallowCall(true)
                        responseBuilder.setRejectCall(true)
                    }
                    RuleAction.ANSWER_THEN_HANG_UP -> {
                        // CallScreeningService cannot programmatically answer calls.
                        // We fall back to REJECT (voicemail) when running outside of default dialer mode.
                        responseBuilder.setDisallowCall(true)
                        responseBuilder.setRejectCall(true)
                        Log.w("CallScreeningService", "ANSWER_THEN_HANG_UP requested in screening mode. Falling back to REJECT.")
                    }
                    else -> {
                        // ALLOW or LOG_ONLY
                    }
                }
                
                respondToCall(callDetails, responseBuilder.build())
            } catch (e: Exception) {
                Log.e("CallScreeningService", "Error screening call", e)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }
}

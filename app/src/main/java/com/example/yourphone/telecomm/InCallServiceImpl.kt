package com.example.yourphone.telecomm

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.example.yourphone.MainActivity
import com.example.yourphone.data.*
import com.example.yourphone.engine.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InCallServiceImpl : InCallService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "InCallServiceImpl"
        private val _currentCall = MutableStateFlow<Call?>(null)
        val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()
        
        @Volatile
        private var instance: InCallServiceImpl? = null

        fun mute(state: Boolean) {
            instance?.setMuted(state)
        }

        fun setRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun getAudioState(): CallAudioState? {
            return instance?.callAudioState
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        
        val handle = call.details.handle
        val rawNumber = handle?.schemeSpecificPart ?: ""
        val direction = call.details.callDirection
        
        Log.d(TAG, "Call added: number=$rawNumber, state=${call.state}, direction=$direction")
        
        if (direction == Call.Details.DIRECTION_OUTGOING) {
            Log.d(TAG, "Skipping rule engine for outgoing call to $rawNumber")
            _currentCall.value = call
            launchUi()
            return
        }
        
        serviceScope.launch {
            try {
                val result = RuleEngine.evaluate(
                    context = this@InCallServiceImpl,
                    rawNumber = rawNumber,
                    target = TargetType.CALL
                )
                
                val database = RulesDatabase.getDatabase(this@InCallServiceImpl)
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

                when (result.action) {
                    RuleAction.ANSWER_THEN_HANG_UP -> {
                        Log.i(TAG, "Executing ANSWER_THEN_HANG_UP for call from $rawNumber")
                        answerAndDisconnect(call)
                    }
                    RuleAction.REJECT -> {
                        Log.i(TAG, "Executing REJECT for call from $rawNumber")
                        call.reject(false, null)
                    }
                    RuleAction.SILENCE -> {
                        _currentCall.value = call
                        launchUi()
                    }
                    else -> {
                        _currentCall.value = call
                        launchUi()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling call in InCallService", e)
                _currentCall.value = call
                launchUi()
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details.handle?.schemeSpecificPart}")
        if (_currentCall.value == call) {
            _currentCall.value = null
        }
    }

    private fun answerAndDisconnect(call: Call) {
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                Log.d(TAG, "Callback call state changed: $state")
                if (state == Call.STATE_ACTIVE) {
                    Log.i(TAG, "Call became active. Hanging up now.")
                    c.disconnect()
                    c.unregisterCallback(this)
                } else if (state == Call.STATE_DISCONNECTED) {
                    c.unregisterCallback(this)
                }
            }
        })
        
        call.answer(0) // 0 represents standard audio-only call connection
    }

    private fun launchUi() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }
}

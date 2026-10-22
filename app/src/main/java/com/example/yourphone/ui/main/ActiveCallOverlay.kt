package com.example.yourphone.ui.main

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yourphone.telecomm.InCallServiceImpl
import kotlinx.coroutines.delay

@Composable
fun ActiveCallOverlay(
    call: Call,
    modifier: Modifier = Modifier
) {
    var callState by remember { mutableStateOf(call.state) }
    val audioState = InCallServiceImpl.getAudioState()
    var isMuted by remember { mutableStateOf(audioState?.isMuted ?: false) }
    var isSpeakerOn by remember { mutableStateOf(audioState?.route == CallAudioState.ROUTE_SPEAKER) }
    var durationSeconds by remember { mutableStateOf(0) }

    DisposableEffect(call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                callState = state
            }
        }
        call.registerCallback(callback)
        onDispose {
            call.unregisterCallback(callback)
        }
    }

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    val number = call.details.handle?.schemeSpecificPart ?: "Unknown Number"
    val stateText = when (callState) {
        Call.STATE_RINGING -> "Incoming Call..."
        Call.STATE_DIALING -> "Dialing..."
        Call.STATE_ACTIVE -> "Active Call - ${formatDuration(durationSeconds)}"
        Call.STATE_DISCONNECTED -> "Disconnected"
        Call.STATE_DISCONNECTING -> "Disconnecting..."
        else -> "Connecting..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight().padding(vertical = 48.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2633)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (number.isNotEmpty()) number.take(1) else "?",
                        color = Color(0xFF81A1C1),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stateText,
                    color = Color(0xFF88C0D0),
                    fontSize = 16.sp
                )
            }

            if (callState == Call.STATE_ACTIVE || callState == Call.STATE_DIALING) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            InCallServiceImpl.mute(isMuted)
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color(0xFFD08770) else Color(0xFF2E3440))
                    ) {
                        Text(text = "Mute", color = Color.White, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = {
                            isSpeakerOn = !isSpeakerOn
                            InCallServiceImpl.setRoute(
                                if (isSpeakerOn) CallAudioState.ROUTE_SPEAKER
                                else CallAudioState.ROUTE_WIRED_OR_EARPIECE
                            )
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color(0xFF8FBCBB) else Color(0xFF2E3440))
                    ) {
                        Text(text = "Speaker", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (callState == Call.STATE_RINGING) {
                    Button(
                        onClick = { call.reject(false, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF616A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Decline", color = Color.White, fontSize = 16.sp)
                    }

                    Button(
                        onClick = { call.answer(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA3BE8C)),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Answer", color = Color.White, fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { call.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF616A)),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("End Call", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

package com.example.yourphone.ui.main

import android.content.Context
import android.net.Uri
import android.telecom.TelecomManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialerTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Number
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFF161B22), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (number.isNotEmpty()) {
                    Text(
                        text = "⌫",
                        color = Color(0xFFBF616A),
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { number = number.dropLast(1) }
                            .padding(8.dp)
                    )
                }
            }
        }

        // Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#")
            )
            
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(0xFF161B22))
                                .clickable { number += key },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (number.isNotEmpty()) {
                        placeOutgoingCall(context, number)
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA3BE8C))
            ) {
                Text("📞", color = Color(0xFF0C0F14), fontSize = 28.sp)
            }
        }
    }
}

private fun placeOutgoingCall(context: Context, number: String) {
    try {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.parse("tel:$number")
        
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Toast.makeText(context, "Grant CALL_PHONE permission to dial numbers.", Toast.LENGTH_LONG).show()
            return
        }

        telecomManager.placeCall(uri, Bundle())
    } catch (e: SecurityException) {
        Log.e("DialerTab", "SecurityException placing call", e)
        Toast.makeText(context, "Permission error: Please grant Phone/Call permission.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.e("DialerTab", "Error placing call", e)
        Toast.makeText(context, "Error dialing: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

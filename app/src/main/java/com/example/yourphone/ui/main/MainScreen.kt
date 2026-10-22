package com.example.yourphone.ui.main

import android.telecom.Call
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.yourphone.telecomm.InCallServiceImpl

enum class Tab {
    LOGS,
    RULES,
    DIALER,
    SETTINGS
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val activeCall by InCallServiceImpl.currentCall.collectAsState()

    if (activeCall != null) {
        ActiveCallOverlay(call = activeCall!!)
    } else {
        var selectedTab by remember { mutableStateOf(Tab.LOGS) }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF161B22)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == Tab.LOGS,
                        onClick = { selectedTab = Tab.LOGS },
                        icon = { Text("📋", fontSize = 20.sp) },
                        label = { Text("Logs") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF88C0D0),
                            indicatorColor = Color(0xFF1F2633)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.RULES,
                        onClick = { selectedTab = Tab.RULES },
                        icon = { Text("🛠️", fontSize = 20.sp) },
                        label = { Text("Rules") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF88C0D0),
                            indicatorColor = Color(0xFF1F2633)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.DIALER,
                        onClick = { selectedTab = Tab.DIALER },
                        icon = { Text("📞", fontSize = 20.sp) },
                        label = { Text("Dialer") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF88C0D0),
                            indicatorColor = Color(0xFF1F2633)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.SETTINGS,
                        onClick = { selectedTab = Tab.SETTINGS },
                        icon = { Text("⚙️", fontSize = 20.sp) },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF88C0D0),
                            indicatorColor = Color(0xFF1F2633)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    Tab.LOGS -> LogsTab()
                    Tab.RULES -> RulesTab()
                    Tab.DIALER -> DialerTab()
                    Tab.SETTINGS -> SettingsTab()
                }
            }
        }
    }
}

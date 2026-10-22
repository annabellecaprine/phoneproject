package com.example.yourphone.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.yourphone.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { RulesDatabase.getDatabase(context) }
    val logs by database.ruleLogDao().getAllLogs().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val totalBlocked = logs.count { 
        it.actionTaken == RuleAction.REJECT || 
        it.actionTaken == RuleAction.ANSWER_THEN_HANG_UP || 
        it.actionTaken == RuleAction.SILENCE 
    }
    
    val totalScreened = logs.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Screened", color = Color.Gray, fontSize = 12.sp)
                    Text("$totalScreened", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Blocked", color = Color.Gray, fontSize = 12.sp)
                    Text("$totalBlocked", color = Color(0xFFBF616A), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Decision Logs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            database.ruleLogDao().clearAllLogs()
                        }
                    }
                ) {
                    Text("Clear All", color = Color(0xFFBF616A))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No log entries yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogItem(
                        log = log,
                        onQuickBlock = { logEntry ->
                            coroutineScope.launch {
                                val action = if (logEntry.targetType == TargetType.CALL) {
                                    RuleAction.ANSWER_THEN_HANG_UP
                                } else {
                                    RuleAction.REJECT
                                }
                                database.ruleDao().insertRule(
                                    Rule(
                                        name = "Quick Block: ${logEntry.rawNumber}",
                                        type = RuleType.BLOCKLIST,
                                        matchingType = MatchingType.EXACT_NUMBER,
                                        pattern = logEntry.rawNumber,
                                        action = action,
                                        targetType = logEntry.targetType
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LogItem(log: RuleLog, onQuickBlock: (RuleLog) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(log.timestamp))
    val context = LocalContext.current
    val isAllowed = log.actionTaken == RuleAction.ALLOW || log.actionTaken == RuleAction.LOG_ONLY

    val badgeColor = when (log.actionTaken) {
        RuleAction.ALLOW -> Color(0xFFA3BE8C)
        RuleAction.SILENCE -> Color(0xFFEBCB8B)
        RuleAction.REJECT -> Color(0xFFBF616A)
        RuleAction.ANSWER_THEN_HANG_UP -> Color(0xFFD08770)
        RuleAction.LOG_ONLY -> Color(0xFF88C0D0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (log.targetType == TargetType.CALL) "📞" else "✉️",
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = log.rawNumber,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = log.actionTaken.name.replace("_", " "),
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.explainableReason,
                color = Color.LightGray,
                fontSize = 13.sp
            )

            if (isAllowed && log.rawNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onQuickBlock(log)
                        android.widget.Toast.makeText(context, "Block rule created for ${log.rawNumber}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("🚫 Block this number", color = Color(0xFFBF616A), fontSize = 12.sp)
                }
            }

            if (log.targetType == TargetType.SMS && !log.messageBody.isNullOrEmpty()) {
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        HorizontalDivider(color = Color(0xFF2E3440))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Message Body:",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.messageBody,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

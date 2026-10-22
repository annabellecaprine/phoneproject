package com.example.yourphone.ui.main

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yourphone.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { RulesDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val roleManager = remember { context.getSystemService(Context.ROLE_SERVICE) as RoleManager }

    var isDialerHeld by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) }
    var isScreenerHeld by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) }
    var isSmsHeld by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_SMS)) }
    var isContactsGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Role request launchers
    val dialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDialerHeld = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    val screenerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isScreenerHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isSmsHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }

    // Contacts permission launcher
    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isContactsGranted = granted
    }

    // Import/Export SAF launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val rules = database.ruleDao().getAllRules().first()
                    val jsonStr = exportRulesToJson(rules)
                    writeTextToUri(context, uri, jsonStr)
                    Toast.makeText(context, "Rules backup exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SettingsTab", "Backup export failed", e)
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = readTextFromUri(context, uri)
                    val rules = importRulesFromJson(jsonStr)
                    withContext(Dispatchers.IO) {
                        rules.forEach { database.ruleDao().insertRule(it) }
                    }
                    Toast.makeText(context, "Successfully imported ${rules.size} rules!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SettingsTab", "Backup import failed", e)
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("System Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // System Roles Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Integrations", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Divider(color = Color(0xFF2E3440))

                // Default Dialer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Dialer Role", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (isDialerHeld) "Active (Answer & Hang Up enabled)" else "Inactive (Voicemail bypass disabled)",
                            color = if (isDialerHeld) Color(0xFFA3BE8C) else Color(0xFFBF616A),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            dialerLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF88C0D0))
                    ) {
                        Text("Set Default", color = Color(0xFF0C0F14), fontSize = 12.sp)
                    }
                }

                // Default Call Screener
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Call Screening Role", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (isScreenerHeld) "Active" else "Inactive",
                            color = if (isScreenerHeld) Color(0xFFA3BE8C) else Color(0xFFBF616A),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            screenerLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF88C0D0))
                    ) {
                        Text("Set Default", color = Color(0xFF0C0F14), fontSize = 12.sp)
                    }
                }

                // Default SMS App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default SMS Handler", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (isSmsHeld) "Active (SMS quarantine enabled)" else "Inactive",
                            color = if (isSmsHeld) Color(0xFFA3BE8C) else Color(0xFFBF616A),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                            smsLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF88C0D0))
                    ) {
                        Text("Set Default", color = Color(0xFF0C0F14), fontSize = 12.sp)
                    }
                }

                // Contacts Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Contacts Provider Access", color = Color.White, fontSize = 14.sp)
                        Text(
                            text = if (isContactsGranted) "Granted (Contacts bypass enabled)" else "Denied",
                            color = if (isContactsGranted) Color(0xFFA3BE8C) else Color(0xFFBF616A),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            contactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF88C0D0))
                    ) {
                        Text("Grant", color = Color(0xFF0C0F14), fontSize = 12.sp)
                    }
                }
            }
        }

        // Backup & Reset Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Maintenance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Divider(color = Color(0xFF2E3440))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("rules_backup.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81A1C1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Backup", color = Color.White)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81A1C1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Backup", color = Color.White)
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                database.clearAllTables()
                            }
                            Toast.makeText(context, "Local configuration reset completely", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF616A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Rules & Logs", color = Color.White)
                }
            }
        }

        // Log Retention Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Log Retention", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Divider(color = Color(0xFF2E3440))

                val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
                var retentionDays by remember { mutableIntStateOf(prefs.getInt("log_retention_days", 0)) }

                Text(
                    text = if (retentionDays == 0) "Logs are kept indefinitely" else "Auto-delete logs older than $retentionDays days",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 30, 60, 90).forEach { days ->
                        FilterChip(
                            selected = retentionDays == days,
                            onClick = {
                                retentionDays = days
                                prefs.edit().putInt("log_retention_days", days).apply()
                                if (days > 0) {
                                    val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.ruleLogDao().pruneOlderThan(cutoff)
                                        }
                                        Toast.makeText(context, "Retention set to $days days", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Logs will be kept indefinitely", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { Text(if (days == 0) "∞" else "${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF88C0D0),
                                selectedLabelColor = Color(0xFF0C0F14)
                            )
                        )
                    }
                }
            }
        }
    }

    // Run auto-cleanup on composition if retention is configured
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val days = prefs.getInt("log_retention_days", 0)
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
            withContext(Dispatchers.IO) {
                database.ruleLogDao().pruneOlderThan(cutoff)
            }
        }
    }
}

// Helpers for Uri File Operations
private suspend fun writeTextToUri(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri)?.use { os ->
        os.write(text.toByteArray(Charsets.UTF_8))
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line).append("\n")
                line = reader.readLine()
            }
        }
    }
    sb.toString()
}

// JSON serialization using Android SDK built-in org.json library
private fun exportRulesToJson(rules: List<Rule>): String {
    val array = JSONArray()
    rules.forEach { rule ->
        val obj = JSONObject().apply {
            put("name", rule.name)
            put("type", rule.type.name)
            put("matchingType", rule.matchingType.name)
            put("pattern", rule.pattern)
            put("action", rule.action.name)
            put("targetType", rule.targetType.name)
            put("isEnabled", rule.isEnabled)
        }
        array.put(obj)
    }
    val envelope = JSONObject().apply {
        put("version", 1)
        put("exportedAt", System.currentTimeMillis())
        put("rules", array)
    }
    return envelope.toString(2)
}

private fun importRulesFromJson(jsonStr: String): List<Rule> {
    val trimmed = jsonStr.trim()
    val array = if (trimmed.startsWith("{")) {
        // Envelope format: { "version": 1, "exportedAt": ..., "rules": [...] }
        val envelope = JSONObject(trimmed)
        envelope.getJSONArray("rules")
    } else {
        // Legacy bare array format for backward compatibility
        JSONArray(trimmed)
    }
    val list = mutableListOf<Rule>()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
            Rule(
                name = obj.getString("name"),
                type = RuleType.valueOf(obj.getString("type")),
                matchingType = MatchingType.valueOf(obj.getString("matchingType")),
                pattern = obj.optString("pattern", ""),
                action = RuleAction.valueOf(obj.getString("action")),
                targetType = TargetType.valueOf(obj.getString("targetType")),
                isEnabled = obj.optBoolean("isEnabled", true)
            )
        )
    }
    return list
}

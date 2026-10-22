package com.example.yourphone.ui.main

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

@Composable
fun RulesTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { RulesDatabase.getDatabase(context) }
    val rules by database.ruleDao().getAllRules().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0C0F14),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF88C0D0),
                contentColor = Color(0xFF0C0F14)
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Rule Engine Config",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No rules configured. Press + to add a rule.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules) { rule ->
                        RuleItem(
                            rule = rule,
                            onToggle = { isEnabled ->
                                coroutineScope.launch {
                                    database.ruleDao().updateRule(rule.copy(isEnabled = isEnabled))
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    database.ruleDao().deleteRule(rule)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newRule ->
                coroutineScope.launch {
                    database.ruleDao().insertRule(newRule)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleItem(
    rule: Rule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = if (rule.type == RuleType.WHITELIST) Color(0xFFA3BE8C) else Color(0xFFBF616A)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rule.type.name,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                val detailsText = when (rule.matchingType) {
                    MatchingType.PRIVATE -> "Matches hidden/private caller"
                    MatchingType.UNKNOWN -> "Matches unknown number (not in contacts)"
                    else -> "${rule.matchingType.name}: ${rule.pattern}"
                }
                
                Text(
                    text = detailsText,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Action: ${rule.action.name.replace("_", " ")} | Channel: ${rule.targetType.name}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                if (rule.timesMatched > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${rule.timesMatched} hit${if (rule.timesMatched != 1L) "s" else ""}",
                        color = Color(0xFF88C0D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF88C0D0),
                        checkedTrackColor = Color(0xFF88C0D0).copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "🗑️",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.BLOCKLIST) }
    var targetType by remember { mutableStateOf(TargetType.CALL) }
    var matchingType by remember { mutableStateOf(MatchingType.EXACT_NUMBER) }
    var pattern by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(RuleAction.ANSWER_THEN_HANG_UP) }

    LaunchedEffect(type) {
        if (type == RuleType.WHITELIST) {
            action = RuleAction.ALLOW
        } else {
            action = RuleAction.ANSWER_THEN_HANG_UP
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Rule", color = Color.White) },
        containerColor = Color(0xFF161B22),
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rule Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF88C0D0),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Rule Type", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RuleType.values().forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (t == RuleType.WHITELIST) Color(0xFFA3BE8C) else Color(0xFFBF616A),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Target Channel", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TargetType.values().forEach { target ->
                            FilterChip(
                                selected = targetType == target,
                                onClick = { targetType = target },
                                label = { Text(target.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF88C0D0),
                                    selectedLabelColor = Color(0xFF0C0F14)
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Matching Mechanism", color = Color.Gray, fontSize = 12.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 3
                    ) {
                        MatchingType.values().forEach { match ->
                            val labelText = if (match == MatchingType.REGEX) "REGEX (Advanced)" else match.name.replace("_", " ")
                            FilterChip(
                                selected = matchingType == match,
                                onClick = { matchingType = match },
                                label = { Text(labelText) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF81A1C1),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (matchingType != MatchingType.PRIVATE && matchingType != MatchingType.UNKNOWN) {
                    item {
                        OutlinedTextField(
                            value = pattern,
                            onValueChange = { pattern = it },
                            label = { 
                                Text(
                                    when (matchingType) {
                                        MatchingType.AREA_CODE -> "Area Code (e.g. 832)"
                                        MatchingType.PREFIX -> "Prefix digits (e.g. +1832555)"
                                        MatchingType.REGEX -> "Regex pattern"
                                        else -> "Phone number pattern"
                                    }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF88C0D0),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (matchingType == MatchingType.REGEX) {
                            Text(
                                text = "⚠️ Regex rules are powerful. Ensure it does not match standard contacts (e.g. avoid overly broad patterns like '.*').",
                                color = Color(0xFFD08770),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Text("Action to Execute", color = Color.Gray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val actionsList = if (type == RuleType.WHITELIST) {
                            listOf(RuleAction.ALLOW, RuleAction.LOG_ONLY)
                        } else {
                            listOf(RuleAction.ANSWER_THEN_HANG_UP, RuleAction.REJECT, RuleAction.SILENCE, RuleAction.LOG_ONLY)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            actionsList.forEach { act ->
                                FilterChip(
                                    selected = action == act,
                                    onClick = { action = act },
                                    label = { Text(act.name.replace("_", " ")) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD08770),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            Rule(
                                name = name,
                                type = type,
                                matchingType = matchingType,
                                pattern = pattern,
                                action = action,
                                targetType = targetType
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF88C0D0))
            ) {
                Text("Save", color = Color(0xFF0C0F14))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

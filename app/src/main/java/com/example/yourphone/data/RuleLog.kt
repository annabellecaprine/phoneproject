package com.example.yourphone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_logs")
data class RuleLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rawNumber: String,
    val normalizedNumber: String,
    val actionTaken: RuleAction,
    val matchedRuleName: String?,
    val explainableReason: String,
    val targetType: TargetType,
    val messageBody: String? = null
)

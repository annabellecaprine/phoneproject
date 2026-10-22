package com.example.yourphone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleType {
    WHITELIST,
    BLOCKLIST
}

enum class MatchingType {
    EXACT_NUMBER,
    PREFIX,
    AREA_CODE,
    REGEX,
    PRIVATE,
    UNKNOWN
}

enum class RuleAction {
    ALLOW,
    SILENCE,
    REJECT,
    ANSWER_THEN_HANG_UP,
    LOG_ONLY
}

enum class TargetType {
    CALL,
    SMS,
    BOTH
}

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    val matchingType: MatchingType,
    val pattern: String,
    val action: RuleAction,
    val targetType: TargetType,
    val isEnabled: Boolean = true,
    val timesMatched: Long = 0
)

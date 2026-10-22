package com.example.yourphone.engine

import android.content.Context
import android.util.Log
import android.telephony.PhoneNumberUtils
import com.example.yourphone.core.ContactsChecker
import com.example.yourphone.core.NormalizationUtil
import com.example.yourphone.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EvaluationResult(
    val action: RuleAction,
    val ruleName: String?,
    val explainableReason: String,
    val ruleId: Long?
)

object RuleEngine {
    private const val TAG = "RuleEngine"

    suspend fun evaluate(
        context: Context,
        rawNumber: String?,
        target: TargetType,
        smsBody: String? = null
    ): EvaluationResult = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        if (!rawNumber.isNullOrBlank() && PhoneNumberUtils.isEmergencyNumber(rawNumber)) {
            return@withContext EvaluationResult(
                action = RuleAction.ALLOW,
                ruleName = "Emergency Safety",
                explainableReason = "Emergency number bypass active for safety",
                ruleId = null
            )
        }

        val database = RulesDatabase.getDatabase(context)
        val ruleDao = database.ruleDao()
        val enabledRules = ruleDao.getEnabledRules()
            .filter { it.targetType == TargetType.BOTH || it.targetType == target }

        val normalized = NormalizationUtil.normalize(rawNumber)
        val isPrivate = rawNumber.isNullOrBlank() || 
                rawNumber.lowercase() in listOf("private", "unknown", "restricted", "withheld", "anonymous")

        Log.d(TAG, "Evaluating event: target=$target, raw=$rawNumber, normalized=$normalized, isPrivate=$isPrivate")

        // 1. Check Private Caller rules
        if (isPrivate) {
            val privateRules = enabledRules.filter { it.matchingType == MatchingType.PRIVATE }
            val whitelistPrivate = privateRules.firstOrNull { it.type == RuleType.WHITELIST }
            if (whitelistPrivate != null) {
                ruleDao.incrementHitCount(whitelistPrivate.id)
                return@withContext EvaluationResult(
                    action = whitelistPrivate.action,
                    ruleName = whitelistPrivate.name,
                    explainableReason = "Private caller matched whitelist rule '${whitelistPrivate.name}'",
                    ruleId = whitelistPrivate.id
                )
            }
            val blocklistPrivate = privateRules.firstOrNull { it.type == RuleType.BLOCKLIST }
            if (blocklistPrivate != null) {
                ruleDao.incrementHitCount(blocklistPrivate.id)
                return@withContext EvaluationResult(
                    action = blocklistPrivate.action,
                    ruleName = blocklistPrivate.name,
                    explainableReason = "Private caller matched blocklist rule '${blocklistPrivate.name}'",
                    ruleId = blocklistPrivate.id
                )
            }
            return@withContext EvaluationResult(
                action = RuleAction.ALLOW,
                ruleName = null,
                explainableReason = "Private caller (no matching rule) -> AllowedByDefault",
                ruleId = null
            )
        }

        // 2. Check Contacts Whitelist
        val inContacts = ContactsChecker.isNumberInContacts(context, rawNumber)
        if (inContacts) {
            return@withContext EvaluationResult(
                action = RuleAction.ALLOW,
                ruleName = "Contacts Whitelist",
                explainableReason = "Caller '$rawNumber' exists in device contacts -> Allowed",
                ruleId = null
            )
        }

        // 3. Evaluate Manual Whitelist Rules
        val whitelistRules = enabledRules.filter { it.type == RuleType.WHITELIST }
        for (rule in whitelistRules) {
            if (isMatch(normalized, rule)) {
                ruleDao.incrementHitCount(rule.id)
                return@withContext EvaluationResult(
                    action = rule.action,
                    ruleName = rule.name,
                    explainableReason = "Number matched manual whitelist rule '${rule.name}' (${rule.matchingType.name}: ${rule.pattern})",
                    ruleId = rule.id
                )
            }
        }

        // 4. Evaluate Manual Blocklist Rules
        val blocklistRules = enabledRules.filter { it.type == RuleType.BLOCKLIST }
        for (rule in blocklistRules) {
            if (rule.matchingType != MatchingType.UNKNOWN && isMatch(normalized, rule)) {
                ruleDao.incrementHitCount(rule.id)
                return@withContext EvaluationResult(
                    action = rule.action,
                    ruleName = rule.name,
                    explainableReason = "Number matched manual blocklist rule '${rule.name}' (${rule.matchingType.name}: ${rule.pattern})",
                    ruleId = rule.id
                )
            }
        }

        // 5. Evaluate Unknown Caller Rule (Not in contacts)
        val unknownRules = enabledRules.filter { it.matchingType == MatchingType.UNKNOWN }
        val blocklistUnknown = unknownRules.firstOrNull { it.type == RuleType.BLOCKLIST }
        if (blocklistUnknown != null) {
            ruleDao.incrementHitCount(blocklistUnknown.id)
            return@withContext EvaluationResult(
                action = blocklistUnknown.action,
                ruleName = blocklistUnknown.name,
                explainableReason = "Unknown caller (not in contacts) matched blocklist rule '${blocklistUnknown.name}'",
                ruleId = blocklistUnknown.id
            )
        }

        // 6. Default Neutral Behavior: ALLOW
        return@withContext EvaluationResult(
            action = RuleAction.ALLOW,
            ruleName = null,
            explainableReason = "No matching rules -> Allowed",
            ruleId = null
        )
    }

    private fun isMatch(normalizedNumber: String, rule: Rule): Boolean {
        return try {
            when (rule.matchingType) {
                MatchingType.EXACT_NUMBER -> {
                    val normalizedPattern = NormalizationUtil.normalize(rule.pattern)
                    NormalizationUtil.numbersMatch(normalizedNumber, normalizedPattern)
                }
                MatchingType.PREFIX -> {
                    val normalizedPattern = NormalizationUtil.normalize(rule.pattern)
                    normalizedNumber.startsWith(normalizedPattern)
                }
                MatchingType.AREA_CODE -> {
                    val normalizedPattern = NormalizationUtil.normalize(rule.pattern)
                    normalizedNumber.startsWith(normalizedPattern) ||
                            normalizedNumber.startsWith("1$normalizedPattern") ||
                            normalizedNumber.startsWith("+1$normalizedPattern")
                }
                MatchingType.REGEX -> {
                    val regex = Regex(rule.pattern)
                    regex.containsMatchIn(normalizedNumber)
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error matching rule: ${rule.name}", e)
            false
        }
    }
}

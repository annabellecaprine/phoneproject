package com.example.yourphone.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRuleType(value: RuleType): String = value.name

    @TypeConverter
    fun toRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun fromMatchingType(value: MatchingType): String = value.name

    @TypeConverter
    fun toMatchingType(value: String): MatchingType = MatchingType.valueOf(value)

    @TypeConverter
    fun fromRuleAction(value: RuleAction): String = value.name

    @TypeConverter
    fun toRuleAction(value: String): RuleAction = RuleAction.valueOf(value)

    @TypeConverter
    fun fromTargetType(value: TargetType): String = value.name

    @TypeConverter
    fun toTargetType(value: String): TargetType = TargetType.valueOf(value)
}

package com.example.yourphone.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<Rule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: Rule): Long

    @Update
    suspend fun updateRule(rule: Rule)

    @Delete
    suspend fun deleteRule(rule: Rule)

    @Query("UPDATE rules SET timesMatched = timesMatched + 1 WHERE id = :ruleId")
    suspend fun incrementHitCount(ruleId: Long)
}

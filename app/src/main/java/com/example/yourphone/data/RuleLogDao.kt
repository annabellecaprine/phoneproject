package com.example.yourphone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleLogDao {
    @Query("SELECT * FROM rule_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<RuleLog>>

    @Query("SELECT * FROM rule_logs WHERE targetType = :targetType ORDER BY timestamp DESC")
    fun getLogsByTarget(targetType: TargetType): Flow<List<RuleLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RuleLog): Long

    @Query("DELETE FROM rule_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM rule_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOlderThan(cutoffTimestamp: Long)
}

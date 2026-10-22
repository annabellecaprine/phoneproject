package com.example.yourphone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Rule::class, RuleLog::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RulesDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun ruleLogDao(): RuleLogDao

    companion object {
        @Volatile
        private var INSTANCE: RulesDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rules ADD COLUMN timesMatched INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): RulesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RulesDatabase::class.java,
                    "rules_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.finnest.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.Executors

/** Draft keys are scoped to a journey so future flows cannot overwrite each other. */
@Entity(primaryKeys = ["journey", "fieldKey"])
data class DraftField(val journey: String, val fieldKey: String, val value: String)

@Dao
interface DraftDao {
    @Query("SELECT * FROM DraftField WHERE journey = :journey")
    fun fields(journey: String): List<DraftField>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(field: DraftField)

    @Query("DELETE FROM DraftField WHERE journey IN (:journeys)")
    fun deleteScopes(journeys: List<String>): Int
}

@Database(entities = [DraftField::class], version = 1, exportSchema = false)
abstract class JourneyDatabase : RoomDatabase() {
    abstract fun drafts(): DraftDao

    companion object {
        @Volatile private var instance: JourneyDatabase? = null
        fun get(context: Context): JourneyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, JourneyDatabase::class.java, "finnest-guest.db")
                .build().also { instance = it }
        }
    }
}

/** Serializes reads and writes across screens and activity recreation. */
object DraftRepository {
    private val executor = Executors.newSingleThreadExecutor()
    private val observers = java.util.concurrent.CopyOnWriteArraySet<(String) -> Unit>()
    fun observeSaved(listener: (String) -> Unit) { observers.add(listener) }
    fun removeSavedObserver(listener: (String) -> Unit) { observers.remove(listener) }
    private fun notifySaved(journey: String) { observers.forEach { it(journey) } }
    /** Durable rows only: failed in-memory edits must never inflate the saved-progress bar. */
    fun loadSaved(context: Context, journey: String, callback: (Result<Map<String, String>>) -> Unit) {
        val app = context.applicationContext
        executor.execute { callback(runCatching { JourneyDatabase.get(app).drafts().fields(journey).associate { it.fieldKey to it.value } }) }
    }
    // Retain failed edits in memory until explicitly retried; never mistake a failed write for a save.
    private val failed = mutableMapOf<Pair<String, String>, String>()
    fun load(context: Context, journey: String, callback: (Result<Map<String, String>>) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            callback(runCatching {
                JourneyDatabase.get(app).drafts().fields(journey).associate { it.fieldKey to it.value } +
                    failed.filterKeys { it.first == journey }.mapKeys { it.key.second }
            })
        }
    }
    fun afterPending(journey: String, callback: (Boolean) -> Unit) {
        executor.execute { callback(failed.keys.none { it.first == journey }) }
    }
    fun retryFailed(context: Context, journey: String, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            failed.filterKeys { it.first == journey }.toMap().forEach { (key, value) ->
                if (runCatching { JourneyDatabase.get(app).drafts().put(DraftField(key.first, key.second, value)) }.isSuccess) failed.remove(key)
            }
            notifySaved(journey)
            callback(failed.keys.none { it.first == journey })
        }
    }
    /** Deletes the requested scopes in one SQL statement on the same serialized queue as writes. */
    fun deleteScopes(context: Context, journeys: Set<String>, callback: (Result<Int>) -> Unit) {
        val app = context.applicationContext
        val scopes = journeys.toSet()
        executor.execute {
            val result = runCatching { JourneyDatabase.get(app).drafts().deleteScopes(scopes.toList()) }
            if (result.isSuccess) { failed.keys.removeAll { it.first in scopes }; scopes.forEach(::notifySaved) }
            callback(result)
        }
    }
    fun save(context: Context, journey: String, key: String, value: String) {
        val app = context.applicationContext
        executor.execute {
            val pair = journey to key
            if (runCatching { JourneyDatabase.get(app).drafts().put(DraftField(journey, key, value)) }.isSuccess) { failed.remove(pair); notifySaved(journey) }
            else failed[pair] = value
        }
    }
}

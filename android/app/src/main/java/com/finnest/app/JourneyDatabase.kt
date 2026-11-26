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

@Entity(primaryKeys = ["userId", "scope", "fieldKey"])
data class PendingAccountField(val userId: String, val scope: String, val fieldKey: String, val value: String)

@Dao
interface DraftDao {
    @Query("SELECT * FROM PendingAccountField WHERE userId = :userId")
    fun pending(userId: String): List<PendingAccountField>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun queue(field: PendingAccountField)
    @Query("DELETE FROM PendingAccountField WHERE userId=:userId AND scope=:scope AND fieldKey=:fieldKey AND value=:value")
    fun ack(userId: String, scope: String, fieldKey: String, value: String)
    @Query("SELECT * FROM DraftField WHERE journey = :journey")
    fun fields(journey: String): List<DraftField>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(field: DraftField)

    @Query("DELETE FROM DraftField WHERE journey IN (:journeys)")
    fun deleteScopes(journeys: List<String>): Int
}

@Database(entities = [DraftField::class, PendingAccountField::class], version = 2, exportSchema = false)
abstract class JourneyDatabase : RoomDatabase() {
    abstract fun drafts(): DraftDao

    companion object {
        @Volatile private var instance: JourneyDatabase? = null
        fun get(context: Context): JourneyDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, JourneyDatabase::class.java, "finnest-guest.db")
                .addMigrations(object: androidx.room.migration.Migration(1,2) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS PendingAccountField (userId TEXT NOT NULL, scope TEXT NOT NULL, fieldKey TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(userId,scope,fieldKey))") } })
                .build().also { instance = it }
        }
    }
}

/** Serializes reads and writes across screens and activity recreation. */
object DraftRepository {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var activeUser: String? = null
    private fun currentScope(journey: String) = activeUser?.let { "account:$it:$journey" } ?: journey
    private val observers = java.util.concurrent.CopyOnWriteArraySet<(String) -> Unit>()
    fun observeSaved(listener: (String) -> Unit) { observers.add(listener) }
    fun removeSavedObserver(listener: (String) -> Unit) { observers.remove(listener) }
    private fun notifySaved(journey: String) { observers.forEach { it(journey) } }
    /** Durable rows only: failed in-memory edits must never inflate the saved-progress bar. */
    fun loadSaved(context: Context, journey: String, callback: (Result<Map<String, String>>) -> Unit) {
        val app = context.applicationContext
        val scoped=AccountClient.scoped(app,journey)
        activeUser=AccountClient.userId(app)
        executor.execute { callback(runCatching { JourneyDatabase.get(app).drafts().fields(scoped).associate { it.fieldKey to it.value } }) }
    }
    // Retain failed edits in memory until explicitly retried; never mistake a failed write for a save.
    private val failed = mutableMapOf<Pair<String, String>, String>()
    fun load(context: Context, journey: String, callback: (Result<Map<String, String>>) -> Unit) {
        val app = context.applicationContext
        val scoped=AccountClient.scoped(app,journey)
        executor.execute {
            callback(runCatching {
                JourneyDatabase.get(app).drafts().fields(scoped).associate { it.fieldKey to it.value } +
                    failed.filterKeys { it.first == scoped }.mapKeys { it.key.second }
            })
        }
    }
    fun afterPending(journey: String, callback: (Boolean) -> Unit) {
        val scoped=currentScope(journey)
        executor.execute { callback(failed.keys.none { it.first == scoped }) }
    }
    fun retryFailed(context: Context, journey: String, callback: (Boolean) -> Unit) {
        val app = context.applicationContext
        val scoped=AccountClient.scoped(app,journey);val user=AccountClient.userId(app)
        executor.execute {
            failed.filterKeys { it.first == scoped }.toMap().forEach { (key, value) ->
                if (runCatching { val db=JourneyDatabase.get(app);db.runInTransaction { db.drafts().put(DraftField(key.first,key.second,value));if(user!=null&&NativeJourneyContract.apply(org.json.JSONObject(),journey,key.second,value))db.drafts().queue(PendingAccountField(user,journey,key.second,value)) } }.isSuccess) failed.remove(key)
            }
            notifySaved(journey)
            if(user!=null)AccountSync.enqueue(app)
            callback(failed.keys.none { it.first == scoped })
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
        val user=AccountClient.userId(app);val scoped=AccountClient.scoped(app,journey)
        activeUser=user
        executor.execute {
            val pair = scoped to key
            if (runCatching { val db=JourneyDatabase.get(app);db.runInTransaction { db.drafts().put(DraftField(scoped,key,value));if(user!=null && NativeJourneyContract.apply(org.json.JSONObject(),journey,key,value))db.drafts().queue(PendingAccountField(user,journey,key,value)) } }.isSuccess) { failed.remove(pair); notifySaved(journey);if(user!=null)AccountSync.enqueue(app) }
            else failed[pair] = value
        }
    }
}

package com.finnest.app

import android.content.Context
import androidx.work.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object AccountSync {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var liveJob: Job?=null
    fun enqueue(context: Context) {
        val user=AccountClient.userId(context) ?: return
        val work=OneTimeWorkRequestBuilder<AccountSyncWorker>().setInputData(workDataOf("userId" to user)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("finnest-sync-$user",ExistingWorkPolicy.APPEND_OR_REPLACE,work)
    }
    fun start(context: Context) {
        val app=context.applicationContext;val user=AccountClient.userId(app) ?: return
        liveJob?.cancel();enqueue(app)
        liveJob=scope.launch {
            val channel=AccountClient.get(app).channel("android-journeys-$user-${java.util.UUID.randomUUID()}")
            try {
                val changes=channel.postgresChangeFlow<PostgresAction>("public") { table="finnest_journeys";filter="user_id=eq.$user" }
                changes.onEach { enqueue(app) }.launchIn(this)
                channel.subscribe(true)
                while(isActive){delay(30000);enqueue(app)}
            } finally { withContext(NonCancellable){runCatching { channel.unsubscribe() }} }
        }
    }
    fun stop(){liveJob?.cancel();liveJob=null}

    // Open screens refresh when another device (the website, or another phone session) changes this account's journey.
    val remoteRevision = java.util.concurrent.atomic.AtomicInteger(0)
    private val remoteListeners = java.util.concurrent.CopyOnWriteArraySet<() -> Unit>()
    fun addRemoteListener(listener: () -> Unit) { remoteListeners.add(listener) }
    fun removeRemoteListener(listener: () -> Unit) { remoteListeners.remove(listener) }
    internal fun notifyRemoteChange() {
        remoteRevision.incrementAndGet()
        android.os.Handler(android.os.Looper.getMainLooper()).post { remoteListeners.forEach { it() } }
    }
    fun request(context: Context, path: String, method: String="GET", body: String?=null): Pair<Int,String> {
        val token=AccountClient.get(context).auth.currentAccessTokenOrNull() ?: return 401 to ""
        val connection=URL(BuildConfig.GUIDANCE_URL+path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod=method;connection.connectTimeout=15000;connection.readTimeout=20000
            connection.setRequestProperty("apikey",BuildConfig.GUIDANCE_PUBLISHABLE_KEY);connection.setRequestProperty("Authorization","Bearer $token");connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Prefer","return=representation")
            if(body!=null){connection.doOutput=true;connection.outputStream.use { it.write(body.toByteArray()) }}
            val status=connection.responseCode;val stream=if(status in 200..299)connection.inputStream else connection.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally { connection.disconnect() }
    }
}

class AccountSyncWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val app=applicationContext;val client=AccountClient.get(app)
        client.auth.awaitInitialization()
        val user=inputData.getString("userId") ?: return Result.success()
        if(AccountClient.userId(app)!=user)return Result.success()
        return try {
            runCatching { client.auth.refreshCurrentSession() }
            for(type in listOf("new","resale")) {
                var synced=false
                for(attempt in 0..2){
                    if(AccountClient.userId(app)!=user)return Result.success()
                    val db=JourneyDatabase.get(app);val pending=db.drafts().pending(user).filter { (if(it.scope=="resale_property") "resale" else "new")==type }
                    val (status,text)=AccountSync.request(app,"/rest/v1/finnest_journeys?user_id=eq.$user&journey_type=eq.$type&select=payload,version,updated_by_device")
                    if(status !in 200..299)return Result.retry()
                    val rows=JSONArray(text);val remote=if(rows.length()>0)rows.getJSONObject(0) else null
                    val payload=remote?.optJSONObject("payload") ?: JSONObject()
                    for(edit in pending)NativeJourneyContract.apply(payload,edit.scope,edit.fieldKey,edit.value)
                    if(pending.isNotEmpty()){
                        val row=JSONObject().put("payload",payload).put("completed_steps",NativeJourneyContract.recordedSteps(type,payload)).put("updated_by_device","android-$user")
                        val version=remote?.optInt("version")
                        val endpoint=if(version!=null)"/rest/v1/finnest_journeys?user_id=eq.$user&journey_type=eq.$type&version=eq.$version" else "/rest/v1/finnest_journeys"
                        if(version==null){row.put("user_id",user);row.put("journey_type",type)}
                        val (saved,output)=AccountSync.request(app,endpoint,if(version==null)"POST" else "PATCH",row.toString())
                        if(saved==409)continue
                        if(saved !in 200..299)return Result.retry()
                        if(JSONArray(output).length()==0)continue
                        db.runInTransaction { pending.forEach { db.drafts().ack(it.userId,it.scope,it.fieldKey,it.value) } }
                        val event=JSONObject().put("user_id",user).put("event","step_saved").put("platform","android").put("detail",JSONObject().put("journey",type).put("steps",NativeJourneyContract.recordedSteps(type,payload)))
                        AccountSync.request(app,"/rest/v1/finnest_activity","POST",event.toString())
                    }
                    if(AccountClient.userId(app)!=user)return Result.success()
                    db.runInTransaction {
                        val scopes=if(type=="resale")listOf("resale_property") else listOf("new_property","local_profile","local_reminders")
                        for(localScope in scopes){
                            val dirty=db.drafts().pending(user).filter { it.scope==localScope }.map { it.fieldKey }.toSet()
                            NativeJourneyContract.decode(JSONObject(payload.toString()),localScope).forEach { (k,v)->if(k !in dirty)db.drafts().put(DraftField("account:$user:$localScope",k,v)) }
                        }
                    }
                    // A newer version written by a different device means an open screen should show it.
                    val seen=app.getSharedPreferences("account_sync",Context.MODE_PRIVATE);val seenKey="seen:$user:$type"
                    val remoteVersion=remote?.optInt("version",-1) ?: -1;val writer=remote?.optString("updated_by_device").orEmpty()
                    val last=seen.getInt(seenKey,Int.MIN_VALUE)
                    if(last!=Int.MIN_VALUE&&remoteVersion!=last&&writer.isNotEmpty()&&writer!="android-$user")AccountSync.notifyRemoteChange()
                    seen.edit().putInt(seenKey,remoteVersion).apply()
                    synced=true;break
                }
                if(!synced)return Result.retry()
            }
            Result.success()
        } catch (_:Exception){Result.retry()}
    }
}
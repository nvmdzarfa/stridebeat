package com.stridebeat.app.data.remote.firebase

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.room.dao.WorkoutDao
import com.stridebeat.app.data.local.room.entity.WorkoutSessionEntity
import com.stridebeat.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutDao: WorkoutDao
) {
    private val prefs = context.getSharedPreferences("stridebeat_profile_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadCachedProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val isFirebaseConfigured: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty() && resolveWebClientId().isNotBlank()
        } catch (_: Exception) {
            false
        }

    init {
        refreshFromFirebaseIfSignedIn()
    }

    private fun resolveWebClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val fromRes = if (resId != 0) context.getString(resId) else ""
        return fromRes.ifBlank { BuildConfig.GOOGLE_WEB_CLIENT_ID }
    }

    private fun buildGoogleSignInOptions(webClientId: String): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    private fun loadCachedProfile(): UserProfile {
        val uid = prefs.getString(KEY_UID, null)
        val name = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val photo = prefs.getString(KEY_PHOTO_URL, null)
        val signedIn = prefs.getBoolean(KEY_GOOGLE_SIGNED_IN, false)
        return UserProfile(
            uid = uid,
            displayName = name,
            email = email,
            photoUrl = photo,
            isGoogleSignedIn = signedIn
        )
    }

    private fun saveCachedProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_UID, profile.uid)
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PHOTO_URL, profile.photoUrl)
            .putBoolean(KEY_GOOGLE_SIGNED_IN, profile.isGoogleSignedIn)
            .apply()
        _userProfile.value = profile
    }

    fun refreshFromFirebaseIfSignedIn() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val cached = _userProfile.value
        val resolvedName = cached.displayName.ifBlank { currentUser.displayName ?: "" }
        saveCachedProfile(
            UserProfile(
                uid = currentUser.uid,
                displayName = resolvedName,
                email = currentUser.email ?: cached.email,
                photoUrl = currentUser.photoUrl?.toString() ?: cached.photoUrl,
                isGoogleSignedIn = true
            )
        )
    }

    suspend fun createGoogleSignInIntent(activityContext: Context): Result<Intent> {
        if (!isFirebaseConfigured) {
            return Result.failure(
                IllegalStateException("Firebase belum dikonfigurasi. Pastikan google-services.json terpasang di build ini.")
            )
        }
        return try {
            val webClientId = resolveWebClientId()
            val client = GoogleSignIn.getClient(activityContext, buildGoogleSignInOptions(webClientId))
            // Clear cached GoogleSignIn account so user can pick their Google account cleanly
            withTimeoutOrNull(2_000L) {
                try { client.signOut().await() } catch (_: Exception) {}
            }
            Result.success(client.signInIntent)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal membuka pemilih akun Google."))
        }
    }

    suspend fun handleGoogleSignInIntentResult(resultCode: Int, data: Intent?): Result<UserProfile> {
        if (!isFirebaseConfigured) {
            return Result.failure(IllegalStateException("Firebase belum dikonfigurasi."))
        }
        if (data == null) {
            return Result.failure(
                Exception(
                    if (resultCode == Activity.RESULT_CANCELED) "Login Google dibatalkan."
                    else "Tidak menerima data login dari Google."
                )
            )
        }

        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
                ?: return Result.failure(IllegalStateException("ID Token Google kosong. Cek SHA-1 & Web Client ID Firebase."))

            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = withTimeout(15_000L) {
                FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
            }
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Gagal mendapatkan data pengguna dari Firebase Auth."))

            val baseProfile = UserProfile(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: account.displayName ?: "",
                email = firebaseUser.email ?: account.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: account.photoUrl?.toString(),
                isGoogleSignedIn = true
            )
            saveCachedProfile(baseProfile)

            // Sinkronkan Firestore dengan timeout supaya UI tidak pernah nge-hang
            val syncResult = withTimeoutOrNull(12_000L) {
                syncAllOnSignIn(baseProfile)
            }
            if (syncResult == null) {
                _syncMessage.value = "Login berhasil! Sinkronisasi Cloud tertunda (cek koneksi)."
            }
            Result.success(_userProfile.value)
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED, 16 -> "Login Google dibatalkan."
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Proses login Google sedang berjalan, coba lagi."
                10 -> "Konfigurasi SHA-1 / OAuth Client di Firebase belum cocok (Code 10)."
                12500 -> "Login Google gagal (Code 12500). Pastikan Google Play Services aktif & support email terisi di Firebase."
                7 -> "Koneksi internet bermasalah saat login Google."
                else -> "Login Google gagal (Code ${e.statusCode}): ${e.localizedMessage ?: "error"}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal login dengan akun Google."))
        }
    }

    suspend fun syncAllOnSignIn(current: UserProfile = _userProfile.value): Result<String> = withContext(Dispatchers.IO) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Firebase belum aktif"))
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Belum login Google"))

        try {
            withTimeout(15_000L) {
                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection("users").document(uid)
                val userSnap = userDocRef.get().await()

            val cloudCustomName = userSnap.getString("customDisplayName")?.takeIf { it.isNotBlank() }
            val resolvedName = cloudCustomName ?: current.displayName.ifBlank {
                FirebaseAuth.getInstance().currentUser?.displayName ?: "Pelari"
            }

            val updatedProfile = current.copy(uid = uid, displayName = resolvedName, isGoogleSignedIn = true)
            saveCachedProfile(updatedProfile)

            // Simpan profil ke Firestore
            val profileMap = hashMapOf(
                "uid" to uid,
                "displayName" to updatedProfile.displayName,
                "customDisplayName" to (cloudCustomName ?: updatedProfile.displayName),
                "email" to updatedProfile.email,
                "photoUrl" to (updatedProfile.photoUrl ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            userDocRef.set(profileMap, SetOptions.merge()).await()

            // Pull semua workout dari cloud & merge ke Room
            val workoutsRef = userDocRef.collection("workouts")
            val cloudSnaps = workoutsRef.get().await()
            val cloudIds = mutableSetOf<String>()

            for (doc in cloudSnaps.documents) {
                val id = doc.getString("id") ?: doc.id
                cloudIds.add(id)
                val stravaId = doc.getString("stravaActivityId")
                val stravaStatus = doc.getString("stravaUploadStatus")
                    ?: if (!stravaId.isNullOrBlank()) "UPLOADED" else "NOT_UPLOADED"
                val entity = WorkoutSessionEntity(
                    id = id,
                    type = doc.getString("type") ?: "RUN",
                    startedAt = doc.getLong("startedAt") ?: 0L,
                    endedAt = doc.getLong("endedAt"),
                    distanceMeters = doc.getDouble("distanceMeters") ?: 0.0,
                    durationMs = doc.getLong("durationMs") ?: 0L,
                    avgPaceMinPerKm = doc.getDouble("avgPaceMinPerKm"),
                    caloriesKcal = doc.getDouble("caloriesKcal"),
                    routePointsJson = doc.getString("routePointsJson") ?: "[]",
                    musicSource = doc.getString("musicSource"),
                    playedTracksJson = doc.getString("playedTracksJson") ?: "[]",
                    stravaActivityId = stravaId,
                    stravaUploadStatus = stravaStatus,
                    isImportedFromStrava = doc.getBoolean("isImportedFromStrava") ?: false,
                    stravaActivityName = doc.getString("stravaActivityName"),
                    gpsInterrupted = doc.getBoolean("gpsInterrupted") ?: false
                )
                workoutDao.insertSession(entity)
            }

            // Push workout lokal yang belum ada di cloud
            val localSessions = workoutDao.getAllSessionsOnce().filterNot { it.id.startsWith("sample") }
            var uploadedCount = 0
            for (local in localSessions) {
                if (!cloudIds.contains(local.id)) {
                    workoutsRef.document(local.id).set(entityToMap(local), SetOptions.merge()).await()
                    uploadedCount++
                }
            }

                val status = "Tersinkron ke Cloud (${cloudSnaps.size()} riwayat)"
                _syncMessage.value = status
                status
            }
            Result.success(_syncMessage.value ?: "Tersinkron ke Cloud")
        } catch (e: Exception) {
            val err = "Gagal sinkronisasi cloud: ${e.localizedMessage ?: "cek koneksi"}"
            _syncMessage.value = err
            Result.failure(Exception(err))
        }
    }

    suspend fun uploadWorkoutEntity(entity: WorkoutSessionEntity) = withContext(Dispatchers.IO) {
        if (FirebaseApp.getApps(context).isEmpty()) return@withContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workouts")
                .document(entity.id)
                .set(entityToMap(entity), SetOptions.merge())
                .await()
            _syncMessage.value = "Sesi terbaru otomatis tersimpan ke Cloud"
        } catch (_: Exception) {
        }
    }

    suspend fun deleteWorkoutFromCloud(sessionId: String) = withContext(Dispatchers.IO) {
        if (FirebaseApp.getApps(context).isEmpty()) return@withContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("workouts")
                .document(sessionId)
                .delete()
                .await()
        } catch (_: Exception) {
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nama tidak boleh kosong"))

        val updated = _userProfile.value.copy(displayName = clean)
        saveCachedProfile(updated)

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(clean)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .set(
                            mapOf(
                                "customDisplayName" to clean,
                                "displayName" to clean,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                    _syncMessage.value = "Nama diperbarui & tersimpan ke Cloud"
                } catch (_: Exception) {
                }
            }
        }
        Result.success(Unit)
    }

    suspend fun signOut() {
        try {
            val webClientId = resolveWebClientId()
            if (webClientId.isNotBlank()) {
                withTimeoutOrNull(2_000L) {
                    GoogleSignIn.getClient(context, buildGoogleSignInOptions(webClientId)).signOut().await()
                }
            }
        } catch (_: Exception) {
        }
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {
            }
        }
        _syncMessage.value = null
        saveCachedProfile(UserProfile())
    }

    private fun entityToMap(entity: WorkoutSessionEntity): Map<String, Any?> {
        return hashMapOf(
            "id" to entity.id,
            "type" to entity.type,
            "startedAt" to entity.startedAt,
            "endedAt" to entity.endedAt,
            "distanceMeters" to entity.distanceMeters,
            "durationMs" to entity.durationMs,
            "avgPaceMinPerKm" to entity.avgPaceMinPerKm,
            "caloriesKcal" to entity.caloriesKcal,
            "routePointsJson" to entity.routePointsJson,
            "musicSource" to entity.musicSource,
            "playedTracksJson" to entity.playedTracksJson,
            "stravaActivityId" to entity.stravaActivityId,
            "stravaUploadStatus" to entity.stravaUploadStatus,
            "isImportedFromStrava" to entity.isImportedFromStrava,
            "stravaActivityName" to entity.stravaActivityName,
            "gpsInterrupted" to entity.gpsInterrupted
        )
    }

    companion object {
        private const val KEY_UID = "profile_uid"
        private const val KEY_DISPLAY_NAME = "profile_display_name"
        private const val KEY_EMAIL = "profile_email"
        private const val KEY_PHOTO_URL = "profile_photo_url"
        private const val KEY_GOOGLE_SIGNED_IN = "profile_google_signed_in"
    }
}

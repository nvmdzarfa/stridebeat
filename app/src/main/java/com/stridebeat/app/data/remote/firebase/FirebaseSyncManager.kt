package com.stridebeat.app.data.remote.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.room.converter.Converters
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutDao: WorkoutDao
) {
    private val prefs = context.getSharedPreferences("stridebeat_profile_prefs", Context.MODE_PRIVATE)
    private val converters = Converters()

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

    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> {
        if (!isFirebaseConfigured) {
            return Result.failure(
                IllegalStateException("Firebase belum dikonfigurasi. Tambahkan google-services.json & rebuild APK.")
            )
        }

        val webClientId = resolveWebClientId()
        val credentialManager = CredentialManager.create(activityContext)

        return try {
            val result = try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                credentialManager.getCredential(activityContext, request)
            } catch (_: NoCredentialException) {
                // Fallback ke Sign-In with Google dialog (penting untuk Xiaomi/Oppo/Realme/Vivo)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                credentialManager.getCredential(activityContext, request)
            }

            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(IllegalStateException("Tipe kredensial Google tidak dikenali."))
            }

            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Gagal mendapatkan data pengguna Google."))

            val baseProfile = UserProfile(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: googleCred.displayName ?: "",
                email = firebaseUser.email ?: googleCred.id,
                photoUrl = firebaseUser.photoUrl?.toString() ?: googleCred.profilePictureUri?.toString(),
                isGoogleSignedIn = true
            )
            saveCachedProfile(baseProfile)

            // Sync profil & workout dari Firestore
            syncAllOnSignIn(baseProfile)
            Result.success(_userProfile.value)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Login Google dibatalkan."))
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
                    stravaActivityId = doc.getString("stravaActivityId"),
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
            Result.success(status)
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
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
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

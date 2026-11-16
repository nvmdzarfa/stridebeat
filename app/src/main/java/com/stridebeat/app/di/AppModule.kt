package com.stridebeat.app.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.data.remote.firebase.FirebaseSyncManager
import com.stridebeat.app.data.remote.spotify.SpotifyAuthManager
import com.stridebeat.app.data.remote.strava.StravaAuthManager
import com.stridebeat.app.data.repository.AuthRepositoryImpl
import com.stridebeat.app.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN stravaUploadStatus TEXT NOT NULL DEFAULT 'NOT_UPLOADED'")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN isImportedFromStrava INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN stravaActivityName TEXT")
            db.execSQL(
                "UPDATE workout_sessions SET stravaUploadStatus = 'UPLOADED' " +
                    "WHERE stravaActivityId IS NOT NULL AND stravaActivityId != ''"
            )
        }
    }

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        tokenStorage: SecureTokenStorage,
        spotifyAuthManager: SpotifyAuthManager,
        stravaAuthManager: StravaAuthManager,
        youTubeMusicAuthManager: com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicAuthManager,
        musicPreferences: com.stridebeat.app.data.local.MusicPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(
            tokenStorage,
            spotifyAuthManager,
            stravaAuthManager,
            youTubeMusicAuthManager,
            musicPreferences
        )
    }


    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context
    ): com.stridebeat.app.domain.repository.MusicRepository {
        return com.stridebeat.app.data.repository.MusicRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideStrideBeatDatabase(
        @ApplicationContext context: Context
    ): com.stridebeat.app.data.local.room.StrideBeatDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.stridebeat.app.data.local.room.StrideBeatDatabase::class.java,
            "stridebeat.db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(
        database: com.stridebeat.app.data.local.room.StrideBeatDatabase
    ): com.stridebeat.app.data.local.room.dao.WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: com.stridebeat.app.data.local.room.dao.WorkoutDao,
        firebaseSyncManager: FirebaseSyncManager
    ): com.stridebeat.app.domain.repository.WorkoutRepository {
        return com.stridebeat.app.data.repository.WorkoutRepositoryImpl(workoutDao, firebaseSyncManager)
    }
}

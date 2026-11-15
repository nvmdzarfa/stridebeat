package com.stridebeat.app.di

import android.content.Context
import com.stridebeat.app.data.local.SecureTokenStorage
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
        stravaAuthManager: StravaAuthManager,
        spotifyAuthManager: SpotifyAuthManager
    ): AuthRepository {
        return AuthRepositoryImpl(tokenStorage, stravaAuthManager, spotifyAuthManager)
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
        ).fallbackToDestructiveMigration().build()
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
        workoutDao: com.stridebeat.app.data.local.room.dao.WorkoutDao
    ): com.stridebeat.app.domain.repository.WorkoutRepository {
        return com.stridebeat.app.data.repository.WorkoutRepositoryImpl(workoutDao)
    }
}

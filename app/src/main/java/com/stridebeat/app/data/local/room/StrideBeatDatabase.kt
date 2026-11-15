package com.stridebeat.app.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stridebeat.app.data.local.room.converter.Converters
import com.stridebeat.app.data.local.room.dao.WorkoutDao
import com.stridebeat.app.data.local.room.entity.WorkoutSessionEntity

@Database(
    entities = [WorkoutSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StrideBeatDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}

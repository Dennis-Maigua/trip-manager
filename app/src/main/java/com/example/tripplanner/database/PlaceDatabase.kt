package com.example.tripplanner.database

import androidx.room.*
import com.example.tripplanner.models.PlaceModel

@Database(entities = [PlaceModel::class], version = 1, exportSchema = false)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}
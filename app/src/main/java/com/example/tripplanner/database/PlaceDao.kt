package com.example.tripplanner.database

import androidx.room.*
import com.example.tripplanner.models.PlaceModel

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceModel)

    @Update
    suspend fun update(place: PlaceModel)

    @Delete
    suspend fun delete(place: PlaceModel)

    @Query("SELECT * FROM places")
    suspend fun getAllPlaces(): List<PlaceModel>

    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun getPlaceById(placeId: Int): PlaceModel
}
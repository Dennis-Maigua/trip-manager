package com.example.tripplanner.models

import androidx.room.*

@Entity(tableName = "places")
data class PlaceModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitude: Double
)
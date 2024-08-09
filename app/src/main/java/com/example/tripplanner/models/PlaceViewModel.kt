package com.example.tripplanner.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.tripplanner.database.PlaceDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application,
        PlaceDatabase::class.java, "place_database"
    ).build()

    private val _places = MutableStateFlow<List<PlaceModel>>(emptyList())
    val places: StateFlow<List<PlaceModel>> = _places.asStateFlow()

    init {
        viewModelScope.launch {
            _places.value = database.placeDao().getAllPlaces()
        }
    }

    fun addPlace(place: PlaceModel) {
        viewModelScope.launch {
            database.placeDao().insert(place)
            _places.value = database.placeDao().getAllPlaces()
        }
    }

    fun updatePlace(place: PlaceModel) {
        viewModelScope.launch {
            database.placeDao().update(place)
            _places.value = database.placeDao().getAllPlaces()
        }
    }

    fun deletePlace(place: PlaceModel) {
        viewModelScope.launch {
            database.placeDao().delete(place)
            _places.value = database.placeDao().getAllPlaces()
        }
    }

    fun getPlace(placeId: Int): Flow<PlaceModel?> {
        return _places.map {
            places -> places.find { it.id == placeId }
        }
    }
}
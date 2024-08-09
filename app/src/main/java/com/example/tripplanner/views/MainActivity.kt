package com.example.tripplanner.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.tripplanner.R
import com.example.tripplanner.models.PlaceModel
import com.example.tripplanner.models.PlaceViewModel
import com.example.tripplanner.ui.theme.TripPlannerTheme
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: PlaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripPlannerTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "places_list") {
                    composable("places_list") { MainScreen(navController, viewModel) }
                    composable("add_place") { AddPlaceScreen(navController, viewModel) }
                    composable("place_detail/{placeId}") { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId")?.toIntOrNull()
                        placeId?.let { PlaceDetailScreen(placeId, navController, viewModel) }
                    }
                    composable("edit_place/{placeId}") { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId")?.toIntOrNull()
                        placeId?.let { EditPlaceScreen(it, navController, viewModel) }
                    }
                    composable("view_map/{placeId}") { backStackEntry ->
                        val placeId = backStackEntry.arguments?.getString("placeId")?.toIntOrNull()
                        placeId?.let { ViewMapScreen(it, navController, viewModel) }
                    }
                }
            }
        }
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.maps_api))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: PlaceViewModel) {
    val appName = stringResource(id = R.string.app_name)
    val places by viewModel.places.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appName,
                        style = TextStyle(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("add_place") }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add_title)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (places.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.add_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray
                )
            }
        }
        else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.card_actions),
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(places, key = { it.id }) { place ->
                        PlaceCard(
                            place = place,
                            onClick = {
                                navController.navigate("place_detail/${place.id}")
                            },
                            onEdit = {
                                coroutineScope.launch {
                                    navController.navigate("edit_place/${place.id}")
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    viewModel.deletePlace(place)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalWearMaterialApi::class)
@Composable
fun PlaceCard(
    place: PlaceModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val anchors = mapOf(0f to 0, 300f to 1, -300f to -1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        val offset = swipeableState.offset.value
        val backgroundColor = when {
            offset > 0 -> Color.Green
            offset < 0 -> Color.Red
            else -> Color.Transparent
        }
        val icon = when {
            offset > 0 -> Icons.Filled.Edit
            offset < 0 -> Icons.Filled.Delete
            else -> null
        }

        // Swipe background color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(if (offset > 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(16.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { onClick() }
                .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = rememberImagePainter(place.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(text = place.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = place.date, fontSize = 16.sp, color = Color.Gray)
                    Text(text = stringResource(id = R.string.view_details), fontSize = 13.sp, color = Color.LightGray)
                }
            }
        }
    }

    LaunchedEffect(swipeableState) {
        snapshotFlow { swipeableState.currentValue }
            .collect { value ->
                if (value == 1) {
                    scope.launch {
                        swipeableState.animateTo(0)
                        onEdit()
                    }
                }
                else if (value == -1) {
                    scope.launch {
                        swipeableState.animateTo(0)
                        onDelete()
                    }
                }
            }
    }
}
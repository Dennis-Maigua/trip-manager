package com.example.tripplanner.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.tripplanner.R
import com.example.tripplanner.models.PlaceModel
import com.example.tripplanner.models.PlaceViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun AddPlaceScreen(
    navController: NavController,
    viewModel: PlaceViewModel
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val selectImageFromGallery = rememberLauncherForActivityResult(contract = PickVisualMedia()) { uri ->
        uri.let {
            imageUri = uri
        }
    }

    val takePhotoWithCamera = rememberLauncherForActivityResult(TakePicture()) { success: Boolean ->
        if (success) {
            imageUri = photoUri
        }
    }

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun createImageFile(): File {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val timeStamp = SimpleDateFormat("yyyyMMdd _HHmmss", Locale.US).format(Date())
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }

    fun selectImage() {
        val items = arrayOf(context.getString(R.string.select_gallery), context.getString(R.string.select_camera))
        AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.select_action))
            setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        selectImageFromGallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                    1 -> {
                        val photoFile = createImageFile()
                        photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        takePhotoWithCamera.launch(photoUri)
                    }
                }
            }
            show()
        }
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { permissionsResult ->
        if (permissionsResult.values.all { it }) {
            selectImage()
        }
        else {
            showSettingsDialog(context)
        }
    }

    fun checkPermissionsAndSelectImage() {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
        else {
            selectImage()
        }
    }

    fun validateAndSubmit(
        imageUri: Uri?,
        name: String,
        description: String,
        date: String,
        location: String
    ): Boolean {
        if (name.isBlank() || description.isBlank() || date.isBlank() || location.isBlank() || imageUri == null) {
            Toast.makeText(context, R.string.fields_required, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.add_title),
                        style = TextStyle(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_btn)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val placeholder = painterResource(id = R.drawable.placeholder)
        val img = imageUri?.let { rememberImagePainter(it) } ?: placeholder

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)   // 3:2
            ) {
                Image(
                    painter = img,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(1.dp)
                        .clickable(onClick = { checkPermissionsAndSelectImage() })
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var title by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var date by remember { mutableStateOf("") }
            var location by remember { mutableStateOf("") }
            var latitude by remember { mutableDoubleStateOf(0.0) }
            var longitude by remember { mutableDoubleStateOf(0.0) }

            TextInputField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(id = R.string.title_text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextInputField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(id = R.string.description_text)
            )

            Spacer(modifier = Modifier.height(16.dp))

            DateInputField(
                context = context,
                value = date,
                onValueChange = { newDate -> date = newDate },
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlacesAutocomplete(
                context = context,
                onPlaceSelected = { loc, lat, long ->
                    location = loc
                    latitude = lat
                    longitude = long
                }
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (validateAndSubmit(imageUri, title, description, date, location)) {
                        val place = PlaceModel(
                            imageUri = imageUri.toString(),
                            title = title,
                            description = description,
                            date = date,
                            location = location,
                            latitude = latitude,
                            longitude = longitude
                        )
                        viewModel.addPlace(place)
                        Toast.makeText(context, R.string.add_success, Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.add_title))
            }
        }
    }
}

@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(40.dp)),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedLabelColor = Color.Red,
            unfocusedLabelColor = Color.Gray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color(0xFFF5F5F5),
            unfocusedContainerColor = Color(0xFFF5F5F5),
        )
    )
}

@Composable
fun DateInputField(
    context: Context,
    value: String,
    onValueChange: (String) -> Unit
) {
    var pickedDate by remember { mutableStateOf(value) }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context, R.style.CustomDatePickerDialog, { _, selectedYear, selectedMonth, selectedDay ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }.time
            pickedDate = dateFormat.format(date)
            onValueChange(pickedDate)
        }, year, month, day)

    TextField(
        value = pickedDate,
        onValueChange = {},
        label = { Text(text = stringResource(id = R.string.date_text)) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(40.dp)),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedLabelColor = Color.Red,
            unfocusedLabelColor = Color.Gray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color(0xFFF5F5F5),
            unfocusedContainerColor = Color(0xFFF5F5F5),
        ),
        trailingIcon = {
            IconButton(
                onClick = { datePickerDialog.show() }
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = stringResource(id = R.string.select_date),
                    tint = Color.Gray
                )
            }
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
fun PlacesAutocomplete(
    context: Context,
    onPlaceSelected: (String, Double, Double) -> Unit
) {
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }

    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val place = Autocomplete.getPlaceFromIntent(data)
                location = place.name ?: ""
                latitude = place.latLng?.latitude ?: 0.0
                longitude = place.latLng?.longitude ?: 0.0
                onPlaceSelected(location, latitude, longitude)
            }
        }
        else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Toast.makeText(context, status.statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

//    val placesClient = Places.createClient(context)
    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.get(0)?.getAddressLine(0) ?: "Address not found"
                } else {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0)
                    } else {
                        "Address not found"
                    }
                }
            } catch (e: Exception) {
                "Unable to get address"
            }
        }
    }

    fun selectCurrentLocation() {
        if (hasLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coroutineScope.launch {
                        val address = getAddressFromLocation(loc.latitude, loc.longitude)

                        location = address
                        latitude = loc.latitude
                        longitude = loc.longitude
                        onPlaceSelected(location, latitude, longitude)
                    }
                } else {
                    Toast.makeText(context, R.string.turn_on, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    fun togglePlacesAutocomplete() {
        val intent = Autocomplete
            .IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(context)
        launcher.launch(intent)
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.fillMaxSize()
    ) {
        TextField(
            value = location,
            onValueChange = { location = it },
            label = { Text(text = stringResource(id = R.string.location_text)) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(40.dp)),
            colors = TextFieldDefaults.colors(
                focusedLabelColor = Color.Red,
                unfocusedLabelColor = Color.Gray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
            ),
            trailingIcon = {
                IconButton(
                    onClick = { togglePlacesAutocomplete() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = stringResource(id = R.string.select_location),
                        tint = Color.Gray
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 12.dp)
                .clickable{ selectCurrentLocation() }
        ) {
            Text(
                text = stringResource(id = R.string.current_location),
                fontSize = 16.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(id = R.string.current_location),
                tint = Color.LightGray
            )
        }
    }
}

private fun showSettingsDialog(context: Context) {
    AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.permissions_title))
        setMessage(context.getString(R.string.permissions_text))
        setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
            openAppSettings(context)
            dialog.dismiss()
        }
        setNegativeButton(context.getString(R.string.cancel_btn)) { dialog, _ ->
            dialog.dismiss()
        }
        create().show()
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
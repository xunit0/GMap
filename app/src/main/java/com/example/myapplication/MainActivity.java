package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.net.Uri;
import android.net.http.UrlRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AuthorAttributions;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.example.myapplication.R.drawable.*;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap map;
    private ActivityMainBinding binding;
    private String endPoint, startingPoint;
    private PolylineOptions currentPolyline;
    private String apiKey = BuildConfig.API_KEY;
    private String sessionKey;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final OkHttpClient client = new OkHttpClient();

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(39.8283, -98.5795);
    private static final int DEFAULT_ZOOM = 5;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] likelyPlaceNames;
    private String[] likelyPlaceAddresses;
    private List[] likelyPlaceAttributions;
    private LatLng[] likelyPlaceLatLngs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        getSessionKey();

        setContentView(view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });




        /* Initializations */
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(this);
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);



        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);




        /* AutoComplete Fragment event listeners */
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            // Specify the types of place data to return - Include more fields for better results
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
            ));
        } else {
            Log.e("AutocompleteError", "Autocomplete fragment is null");
        }
        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                try {
                    // Get info about the selected place
                    Log.i("PlaceSelected", "Place: " + place.getName() + ", " + place.getId());

                    // If you need to move the map to the selected place
                    if (place.getLocation() != null && map != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                place.getLocation(), 15f));
                        Marker marker = map.addMarker(new MarkerOptions().position(place.getLocation()));
                        marker.setTag(place.getId());
                        startingPoint = place.getId();
                        checkAndFetchRoute();
                    }
                } catch (Exception e) {
                    Log.e("PlaceError", "Error processing selected place", e);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                // Handle the error properly
                Log.e("PlaceError", "An error occurred: " + status);
                Toast.makeText(MainActivity.this,
                        "Place selection failed: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        AutocompleteSupportFragment autocompleteFragment2 = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment2);

        if (autocompleteFragment2 != null) {
            // Specify the types of place data to return - Include more fields for better results
            autocompleteFragment2.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
            ));
        } else {
            Log.e("AutocompleteError", "Autocomplete fragment is null");
        }
        autocompleteFragment2.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                try {
                    // Get info about the selected place
                    Log.i("PlaceSelected", "Place: " + place.getName() + ", " + place.getId());

                    // If you need to move the map to the selected place
                    if (place.getLatLng() != null && map != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                place.getLatLng(), 15f));

                        Marker marker = map.addMarker(new MarkerOptions().position(place.getLocation()));
                        marker.setTag(place.getId());

                        endPoint = place.getId();
                        checkAndFetchRoute();
                    }
                } catch (Exception e) {
                    Log.e("PlaceError", "Error processing selected place", e);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                // Handle the error properly
                Log.e("PlaceError", "An error occurred: " + status);
                Toast.makeText(MainActivity.this,
                        "Place selection failed: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Optional: Set hint text
        autocompleteFragment2.setHint("Destination...");


    }


    /* Functions to retrieve route data and print on the map */
    private void checkAndFetchRoute() {
        if (startingPoint != null && endPoint != null) {
            RequestBody body = getFormBody(startingPoint, endPoint);
            fetchRoute(body);
        }
    }
    private RequestBody getFormBody(String origin, String destination) {


        try {
            JSONObject json = new JSONObject();
            JSONObject originObj = new JSONObject();
            originObj.put("placeId", origin);
            JSONObject destinationObj = new JSONObject();
            destinationObj.put("placeId", destination);

            json.put("origin", originObj);
            json.put("destination", destinationObj);
            json.put("travelMode", "DRIVE");

            return RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

    private void fetchRoute(RequestBody body) {


        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("RouteError", "Request failed", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray routes = json.getJSONArray("routes");
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject polyline = route.getJSONObject("polyline");
                        String polylineEncoded = polyline.getString("encodedPolyline");


                        Log.d("JSON response", "onResponse: JSON");


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawRouteOnMap(polylineEncoded);


                            }
                        });

                    } catch (JSONException e) {
                        Log.e("RouteError", "JSON parsing error", e);
                    }
                }
            }
        });
    }

    private void drawRouteOnMap(String encodedPolyline) {
        List<LatLng> path = PolyUtil.decode(encodedPolyline);


        if (currentPolyline != null) {
            currentPolyline = null;
        }

        currentPolyline = new PolylineOptions()
                .addAll(path)
                .color(Color.BLUE)
                .width(10f)
                .geodesic(true);

        map.clear();
        map.addPolyline(currentPolyline);

        Marker startingMarker = map.addMarker(new MarkerOptions().position(path.get(0)));
        startingMarker.setTag(startingPoint);

        Marker endPointMarker = map.addMarker(new MarkerOptions().position(path.get(path.size()-1)));
        endPointMarker.setTag(endPoint);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            boundsBuilder.include(point);
        }
        LatLngBounds bounds = boundsBuilder.build();

        int padding = 100; // adjust as needed
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.animateCamera(cameraUpdate);
        findNearbyGasStations(path.get(0), "Gas Station");
        findNearbyGasStations(path.get(path.size() / 2), "Gas Station");
        findNearbyGasStations(path.get(path.size() - 1), "Gas Station");
    }




    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);


        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        getLocationPermission();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        updateLocationUI();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(defaultLocation)
                .zoom(19)
                .bearing(0)
                .tilt(45)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        map.setOnPolylineClickListener(this);
        map.setOnPolygonClickListener(this);
        map.setOnMarkerClickListener(this);

        // If you want to handle general map clicks, add this:
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                // Handle map click here
                Toast.makeText(MainActivity.this,
                        "Map clicked at: " + latLng.latitude + ", " + latLng.longitude,
                        Toast.LENGTH_SHORT).show();

                //Glide.with(this).load(uri).apply(requestOptions).into(binding.imageView);
            }
        });

    }

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

    }


    private void findNearbyGasStations(LatLng location, String label) {
        OkHttpClient client = new OkHttpClient();

        String apiKey = BuildConfig.MAPS_API_KEY;
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + location.latitude + "," + location.longitude
                + "&radius=5000"
                + "&type=gas_station"
                + "&key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray results = json.getJSONArray("results");

                        for (int i = 0; i < Math.min(3, results.length()); i++) { // top 3 stations
                            JSONObject place = results.getJSONObject(i);
                            JSONObject loc = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = loc.getDouble("lat");
                            double lng = loc.getDouble("lng");
                            String name = place.getString("name");
                            String id = place.getString("place_id");

                            LatLng stationLatLng = new LatLng(lat, lng);

                            runOnUiThread(() -> {
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(stationLatLng)
                                        .title(label + ": " + name));
                                assert marker != null;
                                marker.setTag(id);

                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        String placeId = (String) marker.getTag();
        if (placeId == null) {
            binding.placeCardView.setVisibility(View.GONE);
            return false;
        }

        /* CardView for place details */
        binding.placeCardView.setVisibility(View.VISIBLE);
        fetchPlaceDetailsAndPhoto(placeId);
        binding.imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.placeCardView.setVisibility(View.GONE);
            }
        });

        return true;

    }


    private void fetchPlaceDetailsAndPhoto(String placeId) {
        // Specify the fields to return. Include PHOTO_METADATAS.
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.ADR_FORMAT_ADDRESS,
                Place.Field.PHOTO_METADATAS
        );

        // Construct a request object, passing the place ID and fields array.
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        // Call the PlacesClient and handle the response.
        placesClient.fetchPlace(request)
                .addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse response) {
                        Place place = response.getPlace();
                        List<PhotoMetadata> photoMetadataList = place.getPhotoMetadatas();

                        String addy = place.getAdrFormatAddress();
                        String pla = place.getDisplayName();

                        if (addy != null){
                            binding.addressView.setText(addy);
                        }
                        if (pla != null){
                            binding.displayNameView.setText(pla);
                        }

                        if (photoMetadataList != null && !photoMetadataList.isEmpty()) {
                            // Get the first photo metadata from the list
                            PhotoMetadata photoMetadata = photoMetadataList.get(0);
                            // Use this photoMetadata to fetch the photo URI
                            fetchPhotoUri(photoMetadata);
                        } else {
                            Log.d("Error", "No photo metadata available for this place.");
                            // Handle case where no photo metadata is available
                            Icon icon = Icon.createWithResource(getApplicationContext(), android.R.drawable.ic_menu_report_image);
                            binding.imageView.setImageIcon(icon);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("error", "Place fetch failed: " + exception.getMessage());
                        // Handle the error
                        binding.placeCardView.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchPhotoUri(PhotoMetadata photoMetadata) {
        // Construct a request object for fetching the photo URI
        FetchResolvedPhotoUriRequest photoRequest = FetchResolvedPhotoUriRequest.builder(photoMetadata)
                .setMaxWidth(500) // Optional: Set max width
                .setMaxHeight(300) // Optional: Set max height
                .build();

        // Call the PlacesClient and handle the response.
        placesClient.fetchResolvedPhotoUri(photoRequest)
                .addOnSuccessListener(new OnSuccessListener<FetchResolvedPhotoUriResponse>() {
                    @Override
                    public void onSuccess(FetchResolvedPhotoUriResponse fetchResolvedPhotoUriResponse) {
                        Uri uri = fetchResolvedPhotoUriResponse.getUri();
                        // Use the URI to load the image into an ImageView using an image loading library
                        if (uri != null) {
                            Glide.with(MainActivity.this) // Use your Activity context
                                    .load(uri)
                                    .into(binding.imageView); // Your ImageView
                        } else {
                            Log.d("TAG", "Photo URI is null.");
                            // Handle case where URI is null
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("TAG", "Photo fetch failed: " + exception.getMessage());
                        // Handle the error
                        binding.placeCardView.setVisibility(View.GONE);
                    }
                });


    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()))
                                        .zoom(map.getMaxZoomLevel())
                                        .bearing(0)
                                        .tilt(45)
                                        .build();
                                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), 18));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


//    private void drawTerrainTile(){
//
//        getSessionKey();
//
//        Request request = new Request.Builder()
//                .url("https://tile.googleapis.com/v1/2dtiles/z/x/y")
//                .
//
//    }


    private void getSessionKey(){
        String json = "{ \"mapType\": \"terrain\", \"language\": \"en-US\", \"region\": \"US\", \"layerTypes\": [\"layerRoadmap\"] }";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://tile.googleapis.com/v1/createSession?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("RouteError", "Request failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("JSON response", "onResponse: JSON");
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        sessionKey = jsonObject.getString("session");

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        });
    }

/*
* {
  "session": "AJVsH2wBCPuMtfGhJToX4t-eB8NByEQLpobDeR5Mm23rVDM0MD98F2Mgios9OfejA5keerD2ITVEJ3pd5ABsNTxT6A",
  "expiry": "1749683668",
  "tileWidth": 512,
  "imageFormat": "jpeg",
  "tileHeight": 512
}
* */

}


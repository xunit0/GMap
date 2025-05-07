package com.example.myapplication;

import android.graphics.Color;
import android.net.http.UrlRequest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.BuildConfig;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
                        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener {

    ArrayList<LatLng> mArray = new ArrayList<>();
    GoogleMap map;

    String endPoint, startingPoint;
    PolylineOptions currentPolyline;
    //String apiKey = BuildConfig.API_KEY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        Places.initialize(getApplicationContext(), "AIzaSyBaB50vCT8fFmIq35G6LYW1UGiYv0loKBI");

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
                            map.addMarker(new MarkerOptions().position(place.getLocation()));

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
                            map.addMarker(new MarkerOptions().position(place.getLocation()));

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
            autocompleteFragment.setHint("Search for a place");









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
        OkHttpClient client = new OkHttpClient();

        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", getString(R.string.maps_api_key))
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

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            boundsBuilder.include(point);
        }
        LatLngBounds bounds = boundsBuilder.build();

        int padding = 100; // adjust as needed
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.animateCamera(cameraUpdate);
        findNearbyGasStations(path.get(0), "Gas Station");
        findNearbyGasStations(path.get(path.size()/2), "Gas Station");
        findNearbyGasStations(path.get(path.size()-1), "Gas Station");
    }

    private void checkAndFetchRoute() {
        if (startingPoint != null && endPoint != null) {
            RequestBody body = getFormBody(startingPoint, endPoint);
            fetchRoute(body);
        }
    }





    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

    }

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

    }


    private void findNearbyGasStations(LatLng location, String label) {
        OkHttpClient client = new OkHttpClient();

        String apiKey = getString(R.string.maps_api_key);
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

                            LatLng stationLatLng = new LatLng(lat, lng);

                            runOnUiThread(() -> {
                                map.addMarker(new MarkerOptions()
                                        .position(stationLatLng)
                                        .title(label + ": " + name));
                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


}
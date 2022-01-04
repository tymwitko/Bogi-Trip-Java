package com.app.bogitrip;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionManager;
    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private Button randButton;
    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";
    private TextView debug;
    private Button naviButton;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private TextView errorMessage;
    private Button errorButton;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        naviButton = findViewById(R.id.btnNavi);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        naviButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try {
                    NavigationLauncherOptions options = NavigationLauncherOptions.builder().origin(originPosition).destination(destinationPosition).shouldSimulateRoute(true).build();
                    NavigationLauncher.startNavigation(MainActivity.this, options);
                }
                catch (Exception e)
                {
                    errorMessage();
                }
            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
//        debug = findViewById(R.id.debugView);
//        debug.setText("onMapReady");
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();
//        EditText editMinNumber = findViewById(R.id.editMinNumber);
//        EditText editMaxNumber = findViewById(R.id.editMaxNumber);
//        mapboxMap.addPolygon(generatePerimeter(
//                new LatLng(48.8566d, 2.3522d),
//                Double.parseDouble(editMaxNumber.getText().toString()),
//                64));
    }

    private void enableLocation(){
//        debug = findViewById(R.id.debugView);
//        debug.setText("enableLocation");
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
        }else{
            permissionManager = new PermissionsManager(this);
            permissionManager.requestLocationPermissions(this);
        }
    }
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
//        debug = findViewById(R.id.debugView);
//        debug.setText("initializeLocationEngine");
        int i = 0;
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        while (locationEngine.getLastLocation() == null){
            debug.setText(String.valueOf(i));
            i+=1;
            locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
            locationEngine.activate();
            Location lastLocation = locationEngine.getLastLocation();
            if (lastLocation != null) {
                originLocation = lastLocation;
                setCameraPosition(lastLocation);
            } else {
                locationEngine.addLocationEngineListener(this);
            }
            while (lastLocation == null){
                locationEngine.addLocationEngineListener(this);
                lastLocation = locationEngine.getLastLocation();
                originLocation = lastLocation;
                setCameraPosition(lastLocation);
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13.0));
//        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude(), 13.0)));
    }

    @SuppressLint("MissingPermission")
    @Override
    @SuppressWarnings("MissingPermission")
    public void onMapClick(@NonNull LatLng point) {
        if (destinationMarker != null){
            map.removeMarker(destinationMarker);
        }
        destinationMarker = map.addMarker(new MarkerOptions().position(point));

        destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originLocation = locationEngine.getLastLocation();
        originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());

//        naviButton.setEnabled(true);
//        naviButton.setBackgroundResource(R.color.mapbox_blue);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            originLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //why do we need location - pretty obv, innit?
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted){
            enableLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart(){
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
    }


    //    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//
//    }
//    public void onNaviClick(View view){
//
//    }

    @SuppressWarnings("MissingPermission")
    public void onPrevClick(View view)
    {

//        double value;
//        String text =your_edittext.getText().toString();
//        if(!text.isEmpty())
//            try
//            {
//                value= Double.parseDouble(text);
//                // it means it is double
//            } catch (Exception e1) {
//                // this means it is not double
//                e1.printStackTrace();
//            }

        EditText editMinNumber = findViewById(R.id.editMinNumber);
        EditText editMaxNumber = findViewById(R.id.editMaxNumber);
        if (editMaxNumber.length() != 0 && editMinNumber.length() != 0){
        try{
        if (Double.parseDouble(editMaxNumber.getText().toString()) >= 0 && (Double.parseDouble(editMinNumber.getText().toString()) >= 0)) {
            map.clear();
            double lat = locationEngine.getLastLocation().getLatitude();
            double lon = locationEngine.getLastLocation().getLongitude();
            System.out.print(lat);
            System.out.print(lon);
            map.addPolygon(generatePerimeter(
                    new LatLng(lat,
                            lon),
                    Double.parseDouble(editMaxNumber.getText().toString()),
                    64, "green"));
            map.addPolygon(generatePerimeter(
                    new LatLng(lat,
                            lon),
                    Double.parseDouble(editMinNumber.getText().toString()),
                    64, "red"));
        }else{
            errorMessage();
        }}
        catch (Exception e){
            errorMessage();
            //Log.e(TAG,  "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + String.valueOf(e));
            //debug.setText(String.valueOf(e));
        }}else{
            errorMessage();
        }
//        EditText editTextPersonName = findViewById(R.id.editTextPersonName);
//        TextView txtHello = findViewById(R.id.textView3);
//        txtHello.setText("Say goodbye, " + editTextPersonName.getText().toString());
    }

    private PolygonOptions generatePerimeter(LatLng centerCoordinates, double radiusInKilometers, int numberOfSides, String color) {
        List<LatLng> positions = new ArrayList<>();
        double distanceX = radiusInKilometers / (111.319 * Math.cos(centerCoordinates.getLatitude() * Math.PI / 180));
        double distanceY = radiusInKilometers / 110.574;

        double slice = (2 * Math.PI) / numberOfSides;

        double theta;
        double x;
        double y;
        LatLng position;
        for (int i = 0; i < numberOfSides; ++i) {
            theta = i * slice;
            x = distanceX * Math.cos(theta);
            y = distanceY * Math.sin(theta);

            position = new LatLng(centerCoordinates.getLatitude() + y,
                    centerCoordinates.getLongitude() + x);
            positions.add(position);
        }
        if (color.equals("green")){
            return new PolygonOptions()
                    .addAll(positions)
                    .fillColor(Color.GREEN)
                    .alpha(0.4f);
        }else {
            return new PolygonOptions()
                    .addAll(positions)
                    .fillColor(Color.RED)
                    .alpha(0.4f);
        }
    }


    // https://stackoverflow.com/questions/36905396/randomly-generating-a-latlng-within-a-radius-yields-a-point-out-of-bounds
    @SuppressWarnings("MissingPermission")
    public void onRandClick(View view){
        if (destinationMarker != null) {
            map.removeMarker(destinationMarker);
        }
        EditText editMinNumber = findViewById(R.id.editMinNumber);
        EditText editMaxNumber = findViewById(R.id.editMaxNumber);

//        TextView debug = findViewById(R.id.debugView);
        if (editMaxNumber.length() != 0 && editMinNumber.length() != 0){
            try{
                if (Double.parseDouble(editMaxNumber.getText().toString()) >= 0 && (Double.parseDouble(editMinNumber.getText().toString()) >= 0)) {
                    double maxRadius = Double.parseDouble(editMaxNumber.getText().toString());
                    double minRadius = Double.parseDouble(editMinNumber.getText().toString());
                    Location pin = getLocationInLatLngRad(maxRadius, minRadius, locationEngine.getLastLocation());
                    destinationMarker = map.addMarker(new MarkerOptions().position(new LatLng(pin.getLatitude(), pin.getLongitude())));
                    destinationPosition = Point.fromLngLat(pin.getLongitude(), pin.getLatitude());
                    //        debug.setText(Double.toString(pin.getLatitude()) + " AAAAAAAAA " + Double.toString(pin.getLongitude()));
                    originLocation = locationEngine.getLastLocation();
                    originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
                }else{
                    errorMessage();
                }
            }
            catch (Exception e){
                errorMessage();
                //Log.e(TAG,  "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" + String.valueOf(e));
                //debug.setText(String.valueOf(e));
            }}else{
            errorMessage();
        }

    }


    protected Location getLocationInLatLngRad(double maxRadius, double minRadius, Location currentLocation) { // bylo static
        maxRadius *= 1000;
        minRadius *= 1000;
        double x0 = currentLocation.getLongitude();
        double y0 = currentLocation.getLatitude();

        Random random = new Random();

        // Convert radius from meters to degrees.
        double maxRadiusInDegrees = maxRadius / 111320f;
        double minRadiusInDegrees = minRadius / 111320f;

        // Get a random distance and a random angle.
        double u = random.nextDouble() * (1-minRadius/maxRadius) + minRadius / maxRadius;
        double v = random.nextDouble();
        double w = maxRadiusInDegrees * u;
        double t = 2 * Math.PI * v;
        // Get the x and y delta values.
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        // Compensate the x value.
        double new_x = x / Math.cos(Math.toRadians(y0));

        double foundLatitude;
        double foundLongitude;

        foundLatitude = y0 + y;
        foundLongitude = x0 + new_x;

        Location copy = new Location(currentLocation);
        copy.setLatitude(foundLatitude);
        copy.setLongitude(foundLongitude);
        return copy;
    }
    public void onRouteClick(View view){
        if (destinationMarker != null) {
            getRoute(originPosition, destinationPosition);
        }
    }

    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder().accessToken(Mapbox.getAccessToken()).origin(origin).destination(destination).build().getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.body() == null){
                    Log.e(TAG, "No routes found, check right user and access token");
                    return;
                } else if (response.body().routes().size() == 0){
                    Log.e(TAG, "No routes found");
                    return;
                }
                DirectionsRoute currentRoute = response.body().routes().get(0);

                if (navigationMapRoute != null){
                    navigationMapRoute.removeRoute();
                }else {
                    navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                }
                navigationMapRoute.addRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e(TAG, "Error" + t.getMessage());
            }
        });
    }
    public void errorMessage(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View popupView = getLayoutInflater().inflate(R.layout.popup, null);
        errorButton = popupView.findViewById(R.id.btnok);

        dialogBuilder.setView(popupView);
        dialog = dialogBuilder.create();
        dialog.show();

        errorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}

package com.example.myapplication.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.example.myapplication.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MapRouteActivity extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleMap map;
    double userLat, userLong;
    double Lat, W;
    private LatLng destinationLocation, userLocation;
    private ArrayList<Polyline> polyline = null;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_route);
        Bundle arguments = getIntent().getExtras();
        Lat = Double.parseDouble(arguments.getString("l"));
        W = Double.parseDouble(arguments.getString("w"));

        destinationLocation = new LatLng(W, Lat);

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }

        dialog = new ProgressDialog(MapRouteActivity.this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(destinationLocation);
        markerOptions.icon(setIcon(MapRouteActivity.this, R.drawable.baseline_location_on_24));
        map.addMarker(markerOptions);

        fetchMyLocation();
    }

    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();

                userLocation = new LatLng(userLat, userLong);

                LatLng latLng = new LatLng(userLat, userLong);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(12)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                map.addMarker(new MarkerOptions().position(latLng)
                        .icon(setIcon(MapRouteActivity.this, R.drawable.baseline_location_on_24)));

                getRoute(userLocation, destinationLocation);
            } else {
                Toast.makeText(this, "Не удалось получить текущее местоположение", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRoute(LatLng userLocation, LatLng destinationLocation) {
        dialog.setMessage("Маршрут генерируется, подождите пожалуйста");
        dialog.show();

        RouteDrawing routeDrawing = new RouteDrawing.Builder()
                .context(getApplicationContext())
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(MapRouteActivity.this)
                .alternativeRoutes(true)
                .waypoints(userLocation, destinationLocation)
                .build();
        routeDrawing.execute();
    }

    @Override
    public void onRouteFailure(ErrorHandling e) {
        dialog.dismiss();
        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show();
        if (e != null) {
            Log.e("RouteError", "Ошибка: " + e.getMessage());
        } else {
            Log.e("RouteError", "Что то пошло не так, попробуйте позже");
        }
    }

    @Override
    public void onRouteStart() {
        Toast.makeText(this, "Маршрут строится", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> list, int indexing) {
        Toast.makeText(this, "Маршрут построен", Toast.LENGTH_SHORT).show();
        PolylineOptions polylineOptions = new PolylineOptions();
        ArrayList<Polyline> polylines = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i == indexing) {
                Log.e("TAG", "onRoutingSuccess: routeIndexing" + indexing);
                polylineOptions.color(getResources().getColor(R.color.LightBlue));
                polylineOptions.width(12);
                polylineOptions.addAll(list.get(indexing).getPoints());
                polylineOptions.startCap(new RoundCap());
                polylineOptions.endCap(new RoundCap());
                Polyline polyline = map.addPolyline(polylineOptions);
                polylines.add(polyline);
            }
        }
        dialog.dismiss();
    }

    @Override
    public void onRouteCancelled() {
        dialog.dismiss();
        Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
    }
    public BitmapDescriptor setIcon(Activity context, int drawableID) {
        Drawable drawable = ActivityCompat.getDrawable(context, drawableID);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
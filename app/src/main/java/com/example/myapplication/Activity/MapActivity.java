package com.example.myapplication.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.example.myapplication.Domain.ItemRoute;
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
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private double userLat, userLong;
    private LatLng userLocation;
    private ArrayList<Polyline> polyline = null;
    private ProgressDialog dialog;
    private String bed;
    private ArrayList<LatLng> waypoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_route);

        // Получаем объект из Intent
        ItemRoute itemRoute = (ItemRoute) getIntent().getSerializableExtra("object");
        if (itemRoute == null) {
            Toast.makeText(this, "Ошибка: объект не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Получаем координаты из объекта
        waypoints = getCoordinatesFromObject(itemRoute);
        if (waypoints.isEmpty()) {
            Toast.makeText(this, "Список координат пуст", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

       bed = itemRoute.getBed();
        // Инициализация карты
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }

        dialog = new ProgressDialog(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private ArrayList<LatLng> getCoordinatesFromObject(ItemRoute itemRoute) {
        ArrayList<LatLng> coordinates = new ArrayList<>();
        List<List<Double>> points = itemRoute.getPoints();

        if (points != null && !points.isEmpty()) {
            for (List<Double> point : points) {
                if (point != null && point.size() == 2) {
                    double latitude = point.get(0);
                    double longitude = point.get(1);
                    coordinates.add(new LatLng(latitude, longitude));
                }
            }
        } else {
            Log.e("MapActivity", "Список координат пуст или равен null");
        }

        return coordinates;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Проверка разрешений на доступ к местоположению
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);

            // Добавляем маркеры для всех точек
            for (LatLng point : waypoints) {
                map.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(setIcon(this, R.drawable.baseline_location_on_24)));
            }

            fetchMyLocation();
        } else {
            // Запрашиваем разрешения
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
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

                // Перемещаем камеру на текущее местоположение
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

                // Добавляем маркер для текущего местоположения
                map.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .icon(setIcon(this, R.drawable.baseline_location_on_24)));

                // Строим маршрут через все точки
                getRoute(userLocation, waypoints.get(waypoints.size() - 1), waypoints);
            } else {
                Toast.makeText(this, "Не удалось получить текущее местоположение", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRoute(LatLng start, LatLng end, ArrayList<LatLng> waypoints) {
        dialog.setMessage("Маршрут генерируется, подождите пожалуйста");
        dialog.show();

        if(bed=="Автомобильный") {
            ArrayList<LatLng> allPoints = new ArrayList<>();
            allPoints.add(start);
            allPoints.addAll(waypoints);
            allPoints.add(end);

            RouteDrawing routeDrawing = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(allPoints.toArray(new LatLng[0])) // Передаем все точки
                    .build();
            routeDrawing.execute();
        }
        else {
            ArrayList<LatLng> allPoints = new ArrayList<>();
            allPoints.add(start);
            allPoints.addAll(waypoints);
            allPoints.add(end);

            RouteDrawing routeDrawing = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(AbstractRouting.TravelMode.WALKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(allPoints.toArray(new LatLng[0])) // Передаем все точки
                    .build();
            routeDrawing.execute();
        }
    }

    @Override
    public void onRouteFailure(ErrorHandling e) {
        dialog.dismiss();
        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show();
        if (e != null) {
            Log.e("RouteError", "Ошибка: " + e.getMessage());
        } else {
            Log.e("RouteError", "Что-то пошло не так, попробуйте позже");
        }
    }

    @Override
    public void onRouteStart() {
        Toast.makeText(this, "Маршрут строится", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> list, int indexing) {
        Toast.makeText(this, "Маршрут построен", Toast.LENGTH_SHORT).show();
        if (list != null && !list.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions();
            ArrayList<Polyline> polylines = new ArrayList<>();

            // Получаем точки маршрута
            List<LatLng> points = list.get(indexing).getPoints();
            if (points != null && !points.isEmpty()) {
                // Настраиваем линию маршрута
                polylineOptions.color(getResources().getColor(R.color.LightBlue));
                polylineOptions.width(12);
                polylineOptions.addAll(points);
                polylineOptions.startCap(new RoundCap());
                polylineOptions.endCap(new RoundCap());

                // Добавляем линию на карту
                Polyline polyline = map.addPolyline(polylineOptions);
                polylines.add(polyline);
            } else {
                Log.e("RouteError", "Список точек маршрута пуст");
            }
        } else {
            Log.e("RouteError", "Список маршрутов пуст");
        }
        dialog.dismiss();
    }

    @Override
    public void onRouteCancelled() {
        dialog.dismiss();
        Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
    }

    public BitmapDescriptor setIcon(Activity context, int drawableID) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableID);
        if (drawable == null) {
            throw new IllegalArgumentException("Ресурс не найден: " + drawableID);
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
package com.solovinykray.solovinyykray.Activity;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
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
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;
import com.solovinyykray.solovinyykray.R;


import java.util.ArrayList;

/**
 * Activity для отображения и построения маршрута между текущим местоположением пользователя
 * и заданной точкой назначения на карте Google Maps.
 */

 public class MapRouteActivity extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleMap map;
    double userLat, userLong;
    double Lat, W;
    private LatLng destinationLocation, userLocation;
    private ArrayList<Polyline> polyline = null;
    private ProgressDialog dialog;

    /**
     * Инициализирует активность, получает координаты назначения из Intent
     * и настраивает карту.
     * @param savedInstanceState Сохраненное состояние активности
     */

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

    /**
     * Вызывается когда карта готова к использованию.
     * Настраивает карту, запрашивает разрешения и отображает маркер назначения.
     * @param googleMap Объект GoogleMap
     */

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

    /**
     * Получает текущее местоположение пользователя и инициирует построение маршрута.
     */

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

    /**
     * Инициирует построение маршрута между текущим местоположением и точкой назначения.
     * Автоматически выбирает тип маршрута (пешеходный/автомобильный) в зависимости от расстояния.
     *
     * @param userLocation Текущее местоположение пользователя
     * @param destinationLocation Точка назначения
     */

    private void getRoute(LatLng userLocation, LatLng destinationLocation) {
        dialog.setMessage("Маршрут генерируется, подождите пожалуйста");
        dialog.show();

        double distanceInMeters = SphericalUtil.computeDistanceBetween(userLocation, destinationLocation);

        AbstractRouting.TravelMode travelMode;
        if (distanceInMeters <= 4000) { // 4000 метров = 4 км
            travelMode = AbstractRouting.TravelMode.WALKING;
        } else {
            travelMode = AbstractRouting.TravelMode.DRIVING;
        }

        RouteDrawing routeDrawing = new RouteDrawing.Builder()
                .context(getApplicationContext())
                .travelMode(travelMode)
                .withListener(MapRouteActivity.this)
                .alternativeRoutes(true)
                .waypoints(userLocation, destinationLocation)
                .build();
        routeDrawing.execute();
    }

    /**
     * Обрабатывает ошибку при построении маршрута.
     * @param e Объект ошибки, может быть null
     */

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

    /**
     * Вызывается при начале построения маршрута.
     */

    @Override
    public void onRouteStart() {
        Toast.makeText(this, "Маршрут строится", Toast.LENGTH_SHORT).show();
    }

    /**
     * Вызывается при успешном построении маршрута.
     * Отображает маршрут на карте в виде полилинии.
     * @param list Список возможных маршрутов
     * @param indexing Индекс выбранного маршрута
     */

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

    /**
     * Вызывается при отмене построения маршрута.
     */


    @Override
    public void onRouteCancelled() {
        dialog.dismiss();
        Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
    }

    /**
     * Создает BitmapDescriptor из ресурса drawable для использования в качестве иконки маркера.
     *
     * @param context Контекст активности
     * @param drawableID ID ресурса drawable
     * @return BitmapDescriptor для маркера
     * @throws NullPointerException если drawable не найден
     */

    public BitmapDescriptor setIcon(Activity context, int drawableID) {
        Drawable drawable = ActivityCompat.getDrawable(context, drawableID);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
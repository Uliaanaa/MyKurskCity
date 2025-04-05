package com.example.solovinyykray.Activity;

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
import androidx.core.content.ContextCompat;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.example.solovinyykray.Domain.ItemAttractions;
import com.example.solovinyykray.Domain.ItemRoute;
import com.example.solovinyykray.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity для отображения маршрута на карте Google Maps.
 * Поддерживает построение маршрутов как для заранее заданных точек (ItemRoute),
 * так и для выбранных достопримечательностей (ItemAttractions).
 * Автоматически определяет оптимальный тип маршрута (пешеходный/автомобильный)
 * на основе расстояния между точками.
 */

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

    /**
     * Инициализирует активность, получает данные маршрута из Intent
     * и настраивает карту.
     * @param savedInstanceState Сохраненное состояние активности
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_route);

        ItemRoute itemRoute = (ItemRoute) getIntent().getSerializableExtra("object");
        ArrayList<ItemAttractions> selectedItems = (ArrayList<ItemAttractions>) getIntent().getSerializableExtra("selectedItems");

        if (itemRoute != null) {
            waypoints = getCoordinatesFromObject(itemRoute);
            bed = itemRoute.getBed();
        } else if (selectedItems != null && !selectedItems.isEmpty()) {
            waypoints = getCoordinatesFromItems(selectedItems);
            bed = calculateRouteType(waypoints);
        } else {
            Toast.makeText(this, "Ошибка: данные не переданы", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (waypoints.isEmpty()) {
            Toast.makeText(this, "Список координат пуст", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }

        dialog = new ProgressDialog(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Извлекает координаты из объекта ItemRoute.
     *
     * @param itemRoute Объект маршрута, содержащий точки
     * @return Список координат LatLng
     */

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

    /**
     * Извлекает координаты из списка достопримечательностей.
     *
     * @param items Список объектов ItemAttractions
     * @return Список координат LatLng
     */

    private ArrayList<LatLng> getCoordinatesFromItems(ArrayList<ItemAttractions> items) {
        ArrayList<LatLng> coordinates = new ArrayList<>();
        for (ItemAttractions item : items) {
            double latitude = Double.parseDouble(item.getWidth());
            double longitude = Double.parseDouble(item.getLongitude());
            coordinates.add(new LatLng(latitude, longitude));
        }
        return coordinates;
    }

    /**
     * Определяет тип маршрута (пешеходный/автомобильный) на основе общего расстояния.
     *
     * @param waypoints Список точек маршрута
     * @return Тип маршрута ("Пешеходный" или "Автомобильный")
     */

    private String calculateRouteType(ArrayList<LatLng> waypoints) {
        double totalDistance = 0;
        for (int i = 1; i < waypoints.size(); i++) {
            totalDistance += calculateDistance(waypoints.get(i - 1), waypoints.get(i));
        }

        return (totalDistance > 5000) ? "Автомобильный" : "Пешеходный";
    }

    /**
     * Вычисляет расстояние между двумя точками в метрах.
     *
     * @param start Начальная точка
     * @param end Конечная точка
     * @return Расстояние в метрах
     */

    private double calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

    /**
     * Вызывается когда карта готова к использованию.
     * Настраивает карту, запрашивает разрешения и отображает маркеры.
     *
     * @param googleMap Объект GoogleMap
     */


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);

            for (LatLng point : waypoints) {
                map.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(setIcon(this, R.drawable.baseline_location_on_24)));
            }

            fetchMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
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

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

                map.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .icon(setIcon(this, R.drawable.baseline_location_on_24)));

                getRoute(userLocation, waypoints.get(waypoints.size() - 1), waypoints);
            } else {
                Toast.makeText(this, "Не удалось получить текущее местоположение", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Инициирует построение маршрута между точками с учетом типа маршрута.
     *
     * @param start Начальная точка маршрута
     * @param end Конечная точка маршрута
     * @param waypoints Промежуточные точки маршрута
     */

    private void getRoute(LatLng start, LatLng end, ArrayList<LatLng> waypoints) {
        dialog.setMessage("Маршрут генерируется, подождите пожалуйста");
        dialog.show();

        double distanceToFirstPoint = calculateDistance(start, waypoints.get(0));

        if (bed.equals("Пешеходный") && distanceToFirstPoint > 4000) {
            ArrayList<LatLng> carPoints = new ArrayList<>();
            carPoints.add(start);
            carPoints.add(waypoints.get(0));

            RouteDrawing carRouteDrawing = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(carPoints.toArray(new LatLng[0]))
                    .build();
            carRouteDrawing.execute();

            ArrayList<LatLng> walkingPoints = new ArrayList<>();
            walkingPoints.add(waypoints.get(0));
            walkingPoints.addAll(waypoints.subList(1, waypoints.size()));
            walkingPoints.add(end);

            RouteDrawing walkingRouteDrawing = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(AbstractRouting.TravelMode.WALKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(walkingPoints.toArray(new LatLng[0]))
                    .build();
            walkingRouteDrawing.execute();
        } else {
            ArrayList<LatLng> allPoints = new ArrayList<>();
            allPoints.add(start);
            allPoints.addAll(waypoints);
            allPoints.add(end);

            RouteDrawing routeDrawing = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(bed.equals("Автомобильный") ? AbstractRouting.TravelMode.DRIVING : AbstractRouting.TravelMode.WALKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(allPoints.toArray(new LatLng[0]))
                    .build();
            routeDrawing.execute();
        }
    }

    /**
     * Обрабатывает ошибку при построении маршрута.
     *
     * @param e Объект ошибки
     */

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
     *
     * @param list Список моделей информации о маршруте
     * @param indexing Индекс выбранного маршрута
     */

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> list, int indexing) {
        Toast.makeText(this, "Маршрут построен", Toast.LENGTH_SHORT).show();
        if (list != null && !list.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions();
            ArrayList<Polyline> polylines = new ArrayList<>();

            List<LatLng> points = list.get(indexing).getPoints();
            if (points != null && !points.isEmpty()) {
                polylineOptions.color(getResources().getColor(R.color.LightBlue));
                polylineOptions.width(12);
                polylineOptions.addAll(points);
                polylineOptions.startCap(new RoundCap());
                polylineOptions.endCap(new RoundCap());

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

    /**
     * Вызывается при отмене построения маршрута.
     */

    @Override
    public void onRouteCancelled() {
        dialog.dismiss();
        Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
    }

    /**
     * Вызывается при отмене построения маршрута.
     */

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
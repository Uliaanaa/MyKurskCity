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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.google.android.gms.maps.model.LatLngBounds;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinykray.solovinyykray.Domain.ItemRoute;
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
import com.solovinyykray.solovinyykray.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MapActivity — активность, отображающая маршрут на карте Google Maps с учетом текущего местоположения пользователя.
 * Поддерживает отображение пешеходных и автомобильных маршрутов с расчетом времени и расстояния.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, RouteListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private double userLat, userLong;
    private LatLng userLocation;
    private ProgressDialog dialog;
    private String bed;
    private ArrayList<LatLng> waypoints = new ArrayList<>();
    private int totalDurationInMinutes = 0;
    private AtomicInteger completedRoutes = new AtomicInteger(0);
    private View cardRouteInfo;
    private TextView tvDuration, tvDistance;
    private int totalDistanceInMeters = 0;
    private ProgressBar progressBar;
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private ProgressBar centerProgressBar;
    private AbstractRouting.TravelMode currentTravelMode = AbstractRouting.TravelMode.DRIVING;
    private Button btnDriving, btnWalking;
    private RouteDrawing carRoute, walkRoute, singleRoute;

    /**
     * Инициализация активности. Получает данные маршрута или точек, подготавливает карту и UI.
     * Устанавливает обработчики кнопок для смены типа маршрута.
     *
     * @param savedInstanceState состояние активности при пересоздании
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        cardRouteInfo = findViewById(R.id.cardRouteInfo);
        tvDuration = findViewById(R.id.tvDuration);
        tvDistance = findViewById(R.id.tvDistance);
        progressBar = findViewById(R.id.progressBar);
        centerProgressBar = findViewById(R.id.centerProgressBar);
        btnDriving = findViewById(R.id.btnDriving);
        btnWalking = findViewById(R.id.btnWalking);

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
        } else {
            Toast.makeText(this, "Ошибка инициализации карты", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dialog = new ProgressDialog(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btnDriving.setOnClickListener(v -> {
            currentTravelMode = AbstractRouting.TravelMode.DRIVING;
            if (userLocation != null) {
                clearRouteAndRecalculate();
            } else {
                Toast.makeText(this, "Геолокация недоступна, выберите тип маршрута позже", Toast.LENGTH_SHORT).show();
            }
        });

        btnWalking.setOnClickListener(v -> {
            currentTravelMode = AbstractRouting.TravelMode.WALKING;
            if (userLocation != null) {
                clearRouteAndRecalculate();
            } else {
                Toast.makeText(this, "Геолокация недоступна, выберите тип маршрута позже", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Очищает ресурсы при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearPolylines();
        if (carRoute != null) {
            carRoute.cancel(true);
            carRoute = null;
        }
        if (walkRoute != null) {
            walkRoute.cancel(true);
            walkRoute = null;
        }
        if (singleRoute != null) {
            singleRoute.cancel(true);
            singleRoute = null;
        }
        if (map != null) {
            map.clear();
            map = null;
        }
        fusedLocationProviderClient = null;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Центрирует карту на первой точке маршрута, если геолокация недоступна.
     */
    private void centerMapOnFirstWaypoint() {
        if (map != null && !waypoints.isEmpty()) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(waypoints.get(0))
                    .zoom(12)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        Toast.makeText(this, "Геолокация недоступна, отображена начальная точка маршрута", Toast.LENGTH_SHORT).show();
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Очищает текущие маршруты и инициирует пересчет маршрута в зависимости от текущего типа передвижения.
     */
    private void clearRouteAndRecalculate() {
        clearPolylines();
        totalDurationInMinutes = 0;
        totalDistanceInMeters = 0;
        completedRoutes.set(0);
        cardRouteInfo.setVisibility(View.GONE);
        getRoute(userLocation, waypoints.get(waypoints.size() - 1), waypoints);
    }

    /**
     * Удаляет все ранее отображенные линии маршрутов с карты.
     */
    private void clearPolylines() {
        if (polylines != null) {
            for (Polyline line : polylines) {
                if (line != null) {
                    line.remove();
                }
            }
            polylines.clear();
        }
    }

    /**
     * Преобразует объект маршрута ItemRoute в список координат LatLng.
     *
     * @param itemRoute объект маршрута
     * @return список координат
     */
    private ArrayList<LatLng> getCoordinatesFromObject(ItemRoute itemRoute) {
        ArrayList<LatLng> coordinates = new ArrayList<>();
        List<List<Double>> points = itemRoute.getPoints();
        if (points != null && !points.isEmpty()) {
            for (List<Double> point : points) {
                if (point != null && point.size() >= 2) {
                    coordinates.add(new LatLng(point.get(0), point.get(1)));
                }
            }
        }
        return coordinates;
    }

    /**
     * Получает список координат из выбранных достопримечательностей.
     */
    private ArrayList<LatLng> getCoordinatesFromItems(ArrayList<ItemAttractions> items) {
        ArrayList<LatLng> coordinates = new ArrayList<>();
        for (ItemAttractions item : items) {
            try {
                coordinates.add(new LatLng(
                        Double.parseDouble(item.getWidth()),
                        Double.parseDouble(item.getLongitude())
                ));
            } catch (NumberFormatException e) {
                Log.e("MapActivity", "Неверные координаты для " + item.getTitle());
            }
        }
        return coordinates;
    }

    /**
     * Определяет тип маршрута (пешеходный или автомобильный) на основе общей длины.
     */
    private String calculateRouteType(ArrayList<LatLng> waypoints) {
        double totalDistance = 0;
        for (int i = 1; i < waypoints.size(); i++) {
            totalDistance += calculateDistance(waypoints.get(i - 1), waypoints.get(i));
        }
        return totalDistance > 5000 ? "Автомобильный" : "Пешеходный";
    }

    /**
     * Вычисляет расстояние между двумя координатами в метрах.
     */
    private double calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

    /**
     * Callback, вызываемый после готовности карты Google.
     * Запрашивает разрешения, отображает маркеры и загружает маршрут.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if (map != null && !waypoints.isEmpty()) {
            for (LatLng point : waypoints) {
                map.addMarker(new MarkerOptions().position(point).icon(setIcon(this, R.drawable.baseline_location_on_24)));
            }
            centerMapOnFirstWaypoint();
        }

        map.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            fetchMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Обрабатывает результат запроса разрешений.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && map != null) {
                    map.setMyLocationEnabled(true);
                    fetchMyLocation();
                }
            } else {
                centerMapOnFirstWaypoint();
            }
        }
    }

    /**
     * Получает текущее местоположение пользователя и инициирует построение маршрута.
     */
    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            centerMapOnFirstWaypoint();
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null && map != null) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();
                userLocation = new LatLng(userLat, userLong);

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(userLocation)
                        .zoom(12)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                map.addMarker(new MarkerOptions().position(userLocation)
                        .icon(setIcon(this, R.drawable.baseline_location_on_24)));

                getRoute(userLocation, waypoints.get(waypoints.size() - 1), waypoints);
            } else {
                centerMapOnFirstWaypoint();
            }
        }).addOnFailureListener(e -> {
            Log.e("LocationError", "Ошибка получения местоположения: " + e.getMessage());
            centerMapOnFirstWaypoint();
        });
    }

    /**
     * Построение маршрута между точками. В зависимости от расстояния до первой точки и типа маршрута
     * может быть построено два маршрута (например, сначала на машине, затем пешком).
     */
    private void getRoute(LatLng start, LatLng end, ArrayList<LatLng> waypoints) {

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.VISIBLE);
        }

        double distanceToFirstWaypoint = calculateDistance(start, waypoints.get(0));

        if (bed.equals("Пешеходный") && distanceToFirstWaypoint > 4000) {
            ArrayList<LatLng> carPoints = new ArrayList<>();
            carPoints.add(start);
            carPoints.add(waypoints.get(0));
            carRoute = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(carPoints.toArray(new LatLng[0]))
                    .build();
            carRoute.execute();

            ArrayList<LatLng> walkPoints = new ArrayList<>();
            walkPoints.add(waypoints.get(0));
            walkPoints.addAll(waypoints.subList(1, waypoints.size()));
            walkPoints.add(end);
            walkRoute = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(currentTravelMode)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(walkPoints.toArray(new LatLng[0]))
                    .build();
            walkRoute.execute();
        } else {
            ArrayList<LatLng> allPoints = new ArrayList<>();
            allPoints.add(start);
            allPoints.addAll(waypoints);
            allPoints.add(end);
            singleRoute = new RouteDrawing.Builder()
                    .context(getApplicationContext())
                    .travelMode(currentTravelMode)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(allPoints.toArray(new LatLng[0]))
                    .build();
            singleRoute.execute();
        }
    }

    @Override
    public void onRouteFailure(ErrorHandling e) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
        Toast.makeText(this, "Ошибка построения маршрута", Toast.LENGTH_SHORT).show();
        if (e != null) {
            Log.e("RouteError", "Ошибка: " + e.getMessage());
        } else {
            Log.e("RouteError", "Что-то пошло не так, попробуйте позже");
        }
    }

    /**
     * Callback при начале построения маршрута.
     */
    @Override
    public void onRouteStart() {
        Toast.makeText(this, "Маршрут строится", Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback при успешном построении маршрута. Отображает маршрут, считает общее расстояние и время.
     *
     * @param list список маршрутов
     * @param indexing индекс текущего маршрута
     */
    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> list, int indexing) {
        List<LatLng> routePoints = null;

        if (list != null && !list.isEmpty()) {
            RouteInfoModel routeInfo = list.get(indexing);
            int durationMinutes = routeInfo.getDurationValue() / 60;
            int distanceMeters = routeInfo.getDistanceValue();

            totalDurationInMinutes += durationMinutes;
            totalDistanceInMeters += distanceMeters;

            routePoints = routeInfo.getPoints();
            if (routePoints != null && !routePoints.isEmpty()) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(getResources().getColor(R.color.LightBlue))
                        .width(12)
                        .addAll(routePoints)
                        .startCap(new RoundCap())
                        .endCap(new RoundCap());
                Polyline polyline = map.addPolyline(polylineOptions);
                polylines.add(polyline);
            }
        }

        if (completedRoutes.incrementAndGet() >= ((bed.equals("Пешеходный") && calculateDistance(userLocation, waypoints.get(0)) > 4000) ? 2 : 1)) {
            String durationText = formatDuration(totalDurationInMinutes);
            String distanceText = formatDistance(totalDistanceInMeters);

            tvDuration.setText("Время в пути: " + durationText);
            tvDistance.setText("Расстояние: " + distanceText);
            cardRouteInfo.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            centerProgressBar.setVisibility(View.GONE);
        }

        zoomRoute(routePoints);
    }

    /**
     * Форматирует расстояние в человекочитаемую строку (метры или километры).
     *
     * @param meters расстояние в метрах
     * @return строка с единицами измерения
     */
    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format(Locale.getDefault(), "%.1f км", meters / 1000.0);
        } else {
            return meters + " м";
        }
    }

    /**
     * Масштабирует карту так, чтобы весь маршрут был виден в пределах экрана.
     *
     * @param routePoints список координат маршрута
     */
    private void zoomRoute(List<LatLng> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
    }

    /**
     * Форматирует продолжительность маршрута в часы и минуты.
     *
     * @param minutes продолжительность в минутах
     * @return строка вида "1 час 20 минут"
     */
    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" ").append(getRussianHourWord(hours)).append(" ");
        }
        if (mins > 0 || hours == 0) {
            sb.append(mins).append(" ").append(getRussianMinuteWord(mins));
        }
        return sb.toString().trim();
    }

    /**
     * Возвращает правильную форму слова "час" на русском языке в зависимости от количества.
     *
     * @param hours количество часов
     * @return строка: "час", "часа" или "часов"
     */
    private String getRussianHourWord(int hours) {
        if (hours % 10 == 1 && hours % 100 != 11) return "час";
        if (hours % 10 >= 2 && hours % 10 <= 4 && (hours % 100 < 10 || hours % 100 >= 20)) return "часа";
        return "часов";
    }

    /**
     * Возвращает правильную форму слова "минута" на русском языке в зависимости от количества.
     *
     * @param mins количество минут
     * @return строка: "минута", "минуты" или "минут"
     */
    private String getRussianMinuteWord(int mins) {
        if (mins % 10 == 1 && mins % 100 != 11) return "минута";
        if (mins % 10 >= 2 && mins % 10 <= 4 && (mins % 100 < 10 || mins % 100 >= 20)) return "минуты";
        return "минут";
    }

    /**
     * Callback при отмене построения маршрута.
     */

    @Override
    public void onRouteCancelled() {
        dialog.dismiss();
        progressBar.setVisibility(View.GONE);
        centerProgressBar.setVisibility(View.GONE);
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
        Drawable drawable = ContextCompat.getDrawable(context, drawableID);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
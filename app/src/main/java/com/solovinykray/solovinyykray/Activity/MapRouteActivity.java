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
import com.solovinyykray.solovinyykray.R;

import java.util.ArrayList;

/**
 * Activity для отображения и построения маршрута между текущим местоположением пользователя
 * и заданной точкой назначения на карте Google Maps.
 */
public class MapRouteActivity extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    GoogleMap map;
    double userLat, userLong;
    double Lat, W;
    private LatLng destinationLocation, userLocation;
    private ArrayList<Polyline> polyline = new ArrayList<>();
    private ProgressDialog dialog;
    private AbstractRouting.TravelMode currentTravelMode = AbstractRouting.TravelMode.DRIVING;
    private ProgressBar centerProgressBar;
    private RouteDrawing routeDrawing;

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
        if (arguments != null) {
            Lat = Double.parseDouble(arguments.getString("l"));
            W = Double.parseDouble(arguments.getString("w"));
        } else {
            Toast.makeText(this, "Ошибка: координаты не переданы", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        centerProgressBar = findViewById(R.id.centerProgressBar);

        Button btnDriving = findViewById(R.id.btnDriving);
        Button btnWalking = findViewById(R.id.btnWalking);
        CardView infoCard = findViewById(R.id.infoCard);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        infoCard.setVisibility(View.GONE);

        btnDriving.setOnClickListener(v -> {
            currentTravelMode = AbstractRouting.TravelMode.DRIVING;
            if (userLocation != null) {
                getRoute(userLocation, destinationLocation);
            } else {
                Toast.makeText(this, "Геолокация недоступна, выберите тип маршрута позже", Toast.LENGTH_SHORT).show();
            }
        });

        btnWalking.setOnClickListener(v -> {
            currentTravelMode = AbstractRouting.TravelMode.WALKING;
            if (userLocation != null) {
                getRoute(userLocation, destinationLocation);
            } else {
                Toast.makeText(this, "Геолокация недоступна, выберите тип маршрута позже", Toast.LENGTH_SHORT).show();
            }
        });

        destinationLocation = new LatLng(W, Lat);

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (fragment != null) {
            fragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Ошибка инициализации карты", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dialog = new ProgressDialog(MapRouteActivity.this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
    }

    /**
     * Очищает ресурсы при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (polyline != null) {
            for (Polyline line : polyline) {
                if (line != null) {
                    line.remove();
                }
            }
            polyline.clear();
        }
        if (routeDrawing != null) {
            routeDrawing.cancel(true);
            routeDrawing = null;
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
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchMyLocation();
            } else {
                Toast.makeText(this, "Разрешение на доступ к местоположению не предоставлено", Toast.LENGTH_SHORT).show();
                centerMapOnDestination();
            }
        }
    }

    /**
     * Вызывается, когда карта готова к использованию.
     * Настраивает карту, запрашивает разрешения и отображает маркер назначения.
     * @param googleMap Объект GoogleMap
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(destinationLocation);
        markerOptions.icon(setIcon(MapRouteActivity.this, R.drawable.baseline_location_on_24));
        map.addMarker(markerOptions);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            fetchMyLocation();
        } else {
            centerMapOnDestination();
        }
    }

    /**
     * Центрирует карту на точке назначения, если геолокация недоступна.
     */
    private void centerMapOnDestination() {
        if (map != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(destinationLocation)
                    .zoom(12)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        Toast.makeText(this, "Геолокация недоступна, отображена точка назначения", Toast.LENGTH_SHORT).show();
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Получает текущее местоположение пользователя и инициирует построение маршрута.
     */
    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            centerMapOnDestination();
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLong = location.getLongitude();
                userLocation = new LatLng(userLat, userLong);

                if (map != null) {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(userLocation)
                            .zoom(12)
                            .build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    map.addMarker(new MarkerOptions().position(userLocation)
                            .icon(setIcon(MapRouteActivity.this, R.drawable.baseline_location_on_24)));

                    getRoute(userLocation, destinationLocation);
                }
            } else {
                centerMapOnDestination();
            }
        }).addOnFailureListener(e -> {
            Log.e("LocationError", "Ошибка получения местоположения: " + e.getMessage());
            centerMapOnDestination();
        });
    }

    /**
     * Инициирует построение маршрута между текущим местоположением и точкой назначения.
     * Тип маршрута выбирается пользователем.
     *
     * @param userLocation Текущее местоположение пользователя
     * @param destinationLocation Точка назначения
     */
    private void getRoute(LatLng userLocation, LatLng destinationLocation) {
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.VISIBLE);
        }

        routeDrawing = new RouteDrawing.Builder()
                .context(getApplicationContext())
                .travelMode(currentTravelMode)
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
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        Toast.makeText(this, "Ошибка построения маршрута", Toast.LENGTH_SHORT).show();
        if (e != null) {
            Log.e("RouteError", "Ошибка: " + e.getMessage());
        } else {
            Log.e("RouteError", "Что-то пошло не так, попробуйте позже");
        }
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
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
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }

        if (polyline != null) {
            for (Polyline line : polyline) {
                if (line != null) {
                    line.remove();
                }
            }
            polyline.clear();
        }

        if (list != null && !list.isEmpty()) {
            RouteInfoModel routeInfo = list.get(indexing);
            String duration = routeInfo.getDurationText();
            String durationRu = translateDurationToRussian(duration);
            String routeType = (currentTravelMode == AbstractRouting.TravelMode.WALKING) ? "Пешком" : "На машине";

            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(getResources().getColor(R.color.LightBlue))
                    .width(12)
                    .addAll(routeInfo.getPoints())
                    .startCap(new RoundCap())
                    .endCap(new RoundCap());

            if (map != null) {
                Polyline newLine = map.addPolyline(polylineOptions);
                polyline.add(newLine);

                TextView tvRouteTime = findViewById(R.id.tv_route_time);
                TextView tvRouteMode = findViewById(R.id.tv_route_mode);
                CardView infoCard = findViewById(R.id.infoCard);

                tvRouteTime.setText("Время в пути: " + durationRu);
                tvRouteMode.setText("Тип маршрута: " + routeType);
                infoCard.setVisibility(View.VISIBLE);
            }
        }
    }

    private String translateDurationToRussian(String duration) {
        if (duration == null) return "";
        return duration.replace("hours", "часов")
                .replace("hour", "час")
                .replace("mins", "минут")
                .replace("min", "минута")
                .replace("days", "дней")
                .replace("day", "день");
    }

    /**
     * Вызывается при отмене построения маршрута.
     */
    @Override
    public void onRouteCancelled() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show();
        if (centerProgressBar != null) {
            centerProgressBar.setVisibility(View.GONE);
        }
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
        if (drawable == null) {
            Log.e("MapRouteActivity", "Drawable не найден для ID: " + drawableID);
            return BitmapDescriptorFactory.defaultMarker();
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
package com.solovinykray.solovinyykray.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.solovinykray.solovinyykray.Activity.Detail_AttractionActivity;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinyykray.solovinyykray.R;


import java.util.ArrayList;
import java.util.List;

/**
 * Активность для отображения интерактивной карты с достопримечательностями.
 * Позволяет:
 * - Просматривать метки достопримечательностей на карте
 * - Получать текущее местоположение пользователя
 * - Переходить к детальной информации о достопримечательности при нажатии на маркер
 */
public class MarkerActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final LatLng KURSK_CENTER = new LatLng(51.7304, 36.1926);
    private GoogleMap mMap;
    private DatabaseReference attractionsRef;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<ItemAttractions> allAttractions = new ArrayList<>();

    /**
     * Инициализация активности. Настраивает карту и клиент для определения местоположения.
     *
     * @param savedInstanceState Сохраненное состояние активности
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapError", "Map Fragment is null!");
            Toast.makeText(this, "Ошибка инициализации карты", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Очищает ресурсы при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMap != null) {
            mMap.clear();
            mMap = null;
        }
        fusedLocationProviderClient = null;
        allAttractions.clear();
        attractionsRef = null;
    }

    /**
     * Вызывается при готовности карты. Настраивает карту, запрашивает разрешения,
     * загружает достопримечательности и обрабатывает клики по маркерам.
     *
     * @param googleMap Объект GoogleMap для работы с картой
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(KURSK_CENTER, 12f), 2000, null);

        mMap.setOnMarkerClickListener(marker -> {
            String title = marker.getTitle();
            if (title == null) {
                Log.w("MarkerActivity", "Marker title is null");
                Toast.makeText(this, "Название достопримечательности отсутствует", Toast.LENGTH_SHORT).show();
                return true;
            }

            ItemAttractions selected = findAttractionByTitle(title);
            if (selected != null && selected.getId() != null && selected.getTitle() != null) {
                Intent intent = new Intent(MarkerActivity.this, Detail_AttractionActivity.class);
                intent.putExtra("object", selected);
                intent.putExtra("attractionId", selected.getId());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("MarkerActivity", "Ошибка запуска Detail_AttractionActivity: " + e.getMessage());
                    Toast.makeText(this, "Ошибка открытия деталей", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w("MarkerActivity", "Attraction not found or has null ID/Title for title: " + title);
                Toast.makeText(this, "Достопримечательность не найдена", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fetchMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        loadAttractionsFromFirebase();
    }

    /**
     * Получает текущее местоположение пользователя и центрирует карту на нём.
     */
    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            centerMapOnKursk();
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null && mMap != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(userLocation)
                        .zoom(12)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } else {
                centerMapOnKursk();
            }
        }).addOnFailureListener(e -> {
            Log.e("LocationError", "Ошибка получения местоположения: " + e.getMessage());
            centerMapOnKursk();
        });
    }

    /**
     * Центрирует карту на центре Курска при недоступности геолокации.
     */
    private void centerMapOnKursk() {
        if (mMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(KURSK_CENTER)
                    .zoom(12)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        Toast.makeText(this, "Геолокация недоступна, отображён центр Курска", Toast.LENGTH_SHORT).show();
    }

    /**
     * Обрабатывает результаты запроса разрешений на доступ к местоположению.
     *
     * @param requestCode Код запроса
     * @param permissions Список запрошенных разрешений
     * @param grantResults Результаты запроса
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    fetchMyLocation();
                }
            } else {
                centerMapOnKursk();
            }
        }
    }

    /**
     * Находит достопримечательность по заголовку маркера.
     *
     * @param title Название достопримечательности
     * @return Объект ItemAttractions или null, если не найдено
     */
    private ItemAttractions findAttractionByTitle(String title) {
        for (ItemAttractions item : allAttractions) {
            if (title != null && title.equals(item.getTitle())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Загружает достопримечательности из Firebase и добавляет маркеры на карту.
     */
    private void loadAttractionsFromFirebase() {
        allAttractions.clear();
        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    ItemAttractions attraction = child.getValue(ItemAttractions.class);
                    if (attraction != null && !"rejected".equalsIgnoreCase(attraction.getStatus()) && isValidCoordinates(attraction)) {
                        attraction.setId(child.getKey());
                        allAttractions.add(attraction);
                        double lat = Double.parseDouble(attraction.getWidth());
                        double lng = Double.parseDouble(attraction.getLongitude());
                        LatLng position = new LatLng(lat, lng);
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(attraction.getTitle())
                                    .snippet(attraction.getAddress())
                                    .icon(setIcon(MarkerActivity.this, R.drawable.baseline_location_on_24)));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Ошибка загрузки достопримечательностей: " + error.getMessage());
                Toast.makeText(MarkerActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Создаёт иконку для маркера из ресурса drawable.
     *
     * @param context Контекст приложения
     * @param drawableId Идентификатор ресурса drawable
     * @return Объект BitmapDescriptor для маркера или null в случае ошибки
     */
    private BitmapDescriptor setIcon(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) {
            Log.e("MarkerActivity", "Drawable не найден для ID: " + drawableId);
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Проверяет валидность координат достопримечательности.
     *
     * @param item Объект ItemAttractions
     * @return true, если координаты валидны, false в противном случае
     */
    private boolean isValidCoordinates(ItemAttractions item) {
        try {
            Double.parseDouble(item.getWidth());
            Double.parseDouble(item.getLongitude());
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            Log.e("MarkerActivity", "Невалидные координаты для " + item.getTitle() + ": " + e.getMessage());
            return false;
        }
    }
}

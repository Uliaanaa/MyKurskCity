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

    private GoogleMap mMap;
    private DatabaseReference attractionsRef;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<ItemAttractions> allAttractions = new ArrayList<>();

    /**
     * Вызывается при создании активности.
     * Инициализирует карту и клиент для работы с местоположением.
     *
     * @param savedInstanceState Сохраненное состояние активности
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapError", "Map Fragment is null!");
        }

        attractionsRef = FirebaseDatabase.getInstance().getReference("Attractions");
    }

    /**
     * Вызывается когда карта готова к использованию.
     * Настраивает карту, запрашивает разрешения и загружает данные.
     *
     * @param googleMap Объект GoogleMap, готовый к использованию
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng kurskCenter = new LatLng(51.7304, 36.1926);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(kurskCenter, 12f), 2000, null);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fetchMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        loadAttractionsFromFirebase();

        mMap.setOnMarkerClickListener(marker -> {
            ItemAttractions selected = findAttractionByTitle(marker.getTitle());
            if (selected != null) {
                Intent intent = new Intent(MarkerActivity.this, Detail_AttractionActivity.class);
                intent.putExtra("object", selected);
                startActivity(intent);
            }
            return true;
        });
    }

    /**
     * Получает текущее местоположение пользователя и центрирует карту на нем.
     * Требует разрешения ACCESS_FINE_LOCATION.
     */
    private void fetchMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                double userLat = location.getLatitude();
                double userLong = location.getLongitude();
                LatLng userLocation = new LatLng(userLat, userLong);

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(userLocation)
                        .zoom(12)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }

    /**
     * Обрабатывает результат запроса разрешений.
     *
     * @param requestCode Код запроса
     * @param permissions Запрошенные разрешения
     * @param grantResults Результаты запроса
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    fetchMyLocation();
                }
            }
        }
    }

    /**
     * Находит достопримечательность по заголовку маркера.
     *
     * @param title Заголовок маркера (название достопримечательности)
     * @return Найденный объект ItemAttractions или null если не найден
     */
    private ItemAttractions findAttractionByTitle(String title) {
        for (ItemAttractions item : allAttractions) {
            if (item.getTitle().equals(title)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Загружает данные о достопримечательностях из Firebase и добавляет маркеры на карту.
     */
    private void loadAttractionsFromFirebase() {
        allAttractions.clear();
        attractionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    ItemAttractions attraction = child.getValue(ItemAttractions.class);
                    if (attraction != null && isValidCoordinates(attraction)) {
                        allAttractions.add(attraction);
                        double lat = Double.parseDouble(attraction.getWidth());
                        double lng = Double.parseDouble(attraction.getLongitude());

                        LatLng position = new LatLng(lat, lng);
                        mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(attraction.getTitle())
                                .snippet(attraction.getAddress())
                                .icon(setIcon(MarkerActivity.this, R.drawable.baseline_location_on_24)));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MarkerActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Создает иконку для маркера из ресурса drawable.
     *
     * @param context Контекст приложения
     * @param drawableId ID ресурса drawable
     * @return BitmapDescriptor созданный из drawable или null в случае ошибки
     */
    private BitmapDescriptor setIcon(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Проверяет валидность координат достопримечательности.
     *
     * @param item Проверяемый объект ItemAttractions
     * @return true если координаты валидны, false в противном случае
     */
    private boolean isValidCoordinates(ItemAttractions item) {
        try {
            Double.parseDouble(item.getWidth());
            Double.parseDouble(item.getLongitude());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.solovinykray.solovinyykray.Domain.Attractions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.solovinyykray.solovinyykray.R;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Активность для подачи заявки на добавление достопримечательности.
 * Позволяет пользователю заполнить форму с информацией о достопримечательности,
 * загрузить изображение и отправить заявку на модерацию.
 * Автоматически определяет координаты по указанному адресу с помощью Google Maps API.
 */
public class FormActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextAddress, editTextDescription, editTextType;
    private Button buttonSave, buttonSelectImage;
    private ImageView imageViewSelected, backBtn;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Uri imageUri;
    private final String defaultImageUrl = "https://firebasestorage.googleapis.com/v0/b/solovinyy-kray.firebasestorage.app/o/%D0%9F%D0%B0%D0%BC%D1%8F%D1%82%D0%BD%D0%B8%D0%BA%D0%B8.jpg?alt=media&token=e78f8904-f935-43f6-b53d-fc1ca90ee1c6";

    /**
     * Инициализирует UI элементы,
     * настраивает слушатели событий и проверяет авторизацию пользователя.
     *
     * @param savedInstanceState Сохраненное состояние активности
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        enableImmersiveMode();

        databaseReference = FirebaseDatabase.getInstance().getReference("PendingAttractions");
        storageReference = FirebaseStorage.getInstance().getReference("attractions_images");

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextType = findViewById(R.id.editTextType);
        buttonSave = findViewById(R.id.buttonSave);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        imageViewSelected = findViewById(R.id.imageViewSelected);
        backBtn = findViewById(R.id.backBtn);

        Glide.with(this)
                .load(defaultImageUrl)
                .into(imageViewSelected);
        imageViewSelected.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Чтобы отправить форму, войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(FormActivity.this, LoginActivity.class));
            finish();
        }

        buttonSelectImage.setOnClickListener(v -> ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start());

        buttonSave.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Чтобы отправить форму, войдите в систему", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(FormActivity.this, LoginActivity.class));
                finish();
            } else {
                saveAttraction();
            }
        });

        backBtn.setOnClickListener(v -> finish());
    }

    /**
     * Обрабатывает результат выбора изображения из галереи или камеры.
     *
     * @param requestCode Код запроса
     * @param resultCode Код результата
     * @param data Данные с выбранным изображением
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageViewSelected.setImageURI(imageUri);
            imageViewSelected.setVisibility(View.VISIBLE);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Задача отменена", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Скрывает системную навигацию (включает иммерсивный режим).
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Проверяет заполнение полей формы и инициирует процесс сохранения.
     */
    private void saveAttraction() {
        String title = editTextTitle.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String type = editTextType.getText().toString().trim();

        if (title.isEmpty() || address.isEmpty() || description.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        String attractionId = UUID.randomUUID().toString();
        saveAttractionWithKey(attractionId);
    }

    /**
     * Сохраняет достопримечательность с указанным ключом, предварительно получив координаты.
     *
     * @param attractionId Уникальный строковый идентификатор для новой записи
     */
    private void saveAttractionWithKey(String attractionId) {
        getCoordinates(editTextAddress.getText().toString().trim(), (latitude, longitude) -> {
            String latitudeStr = String.format(Locale.US, "%.6f", latitude);
            String longitudeStr = String.format(Locale.US, "%.6f", longitude);

            if (imageUri == null) {
                saveAttractionToDatabase(attractionId, latitudeStr, longitudeStr, defaultImageUrl);
            } else {
                StorageReference fileReference = storageReference.child(attractionId + ".jpg");
                fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveAttractionToDatabase(attractionId, latitudeStr, longitudeStr, imageUrl);
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка при загрузке изображения", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Сохраняет данные достопримечательности в Firebase Database.
     *
     * @param attractionId Уникальный строковый идентификатор записи
     * @param latitudeStr Широта в строковом формате
     * @param longitudeStr Долгота в строковом формате
     * @param imageUrl URL изображения
     */
    private void saveAttractionToDatabase(String attractionId, String latitudeStr, String longitudeStr, String imageUrl) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "unknown";

        Attractions attraction = new Attractions(
                editTextTitle.getText().toString().trim(),
                editTextAddress.getText().toString().trim(),
                editTextDescription.getText().toString().trim(),
                editTextType.getText().toString().trim(),
                latitudeStr,
                longitudeStr,
                0,
                imageUrl,
                "pending",
                userId
        );

        DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("PendingAttractions");
        pendingRef.child(attractionId).setValue(attraction)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Заявка отправлена на модерацию", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка при сохранении заявки", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Получает географические координаты по адресу с помощью Google Maps Geocoding API.
     *
     * @param address Адрес для геокодирования
     * @param callback Callback для обработки результата
     */
    private void getCoordinates(String address, GeocodingCallback callback) {
        String apiKey = "AIzaSyCli7GThQM59jg-MG0TenJcB6bvBomrIcQ";
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + URLEncoder.encode(address, StandardCharsets.UTF_8)
                + "&key=" + apiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(FormActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(FormActivity.this, "Ошибка запроса: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String jsonData = response.body().string();
                response.close();
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray results = jsonObject.optJSONArray("results");

                    if (results != null && results.length() > 0) {
                        JSONObject location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location");

                        double latitude = location.getDouble("lat");
                        double longitude = location.getDouble("lng");

                        runOnUiThread(() -> callback.onCoordinatesReceived(latitude, longitude));
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(FormActivity.this, "Адрес не найден", Toast.LENGTH_SHORT).show()
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(FormActivity.this, "Ошибка обработки JSON", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /**
     * Интерфейс callback для получения координат из Geocoding API.
     */
    interface GeocodingCallback {
        /**
         * Вызывается при успешном получении координат.
         * @param latitude Широта
         * @param longitude Долгота
         */
        void onCoordinatesReceived(double latitude, double longitude);
    }
}
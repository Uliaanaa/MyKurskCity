package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.solovinykray.solovinyykray.Domain.ItemAttractions;
import com.solovinykray.solovinyykray.Domain.Review;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailAttractionBinding;


/**
 * Активность для просмотра детальной информации о достопримечательности,
 * позволяет просмотреть описание, адрес, тип достопримечательности, рейтинг, изображения и отзывы.
 * Поддерживает функцию добавления в избранное, переход к карте и переход к разделу с отзывами.
 */
public class Detail_AttractionActivity extends BaseActivity {
    private ActivityDetailAttractionBinding binding;
    private ItemAttractions object;
    private boolean isFavorite = false;
    String l = "", w = "";
    private TextView videoTitle;

    /**
     * Инициализирует активность, привязку данных, получает переданные данные,
     * настраивает интерфейс и загружает рейтинг.
     * @param savedInstanceState Сохраненное состояние активности (может быть null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailAttractionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentExtra();
        setVariable();
        loadAndCalculateRating();
        enableImmersiveMode();
        setupVkVideoPlayer();
    }

    /**
     * Настраивает WebView для воспроизведения видео с VK Video.
     */
    private void setupVkVideoPlayer() {
        String videoUrl = object.getVideoUrl();

        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.d("VkVideoDebug", "Ссылка на видео отсутствует");
            binding.vkWebView.setVisibility(View.GONE);
            binding.videoTitle.setVisibility(View.GONE);
            return;
        }

        String embedUrl = extractVkVideoEmbedUrl(videoUrl);
        Log.d("VkVideoDebug", "Embed URL: " + embedUrl);

        if (embedUrl == null || embedUrl.isEmpty()) {
            Log.e("VkVideoError", "Неверный формат VK Video ссылки: " + videoUrl);
            binding.vkWebView.setVisibility(View.GONE);
            binding.videoTitle.setVisibility(View.GONE);
            return;
        }

        binding.videoTitle.setVisibility(View.VISIBLE);
        binding.vkWebView.setVisibility(View.VISIBLE);

        binding.vkWebView.getSettings().setJavaScriptEnabled(true);
        binding.vkWebView.getSettings().setDomStorageEnabled(true);
        binding.vkWebView.getSettings().setLoadWithOverviewMode(true);
        binding.vkWebView.getSettings().setUseWideViewPort(true);

        binding.vkWebView.setWebChromeClient(new WebChromeClient());

        binding.vkWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("VkVideoDebug", "Страница загружена: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e("VkVideoError", "Ошибка загрузки WebView: " + description);
                binding.vkWebView.setVisibility(View.GONE);
                binding.videoTitle.setVisibility(View.GONE);
            }
        });

        Log.d("VkVideoDebug", "Загружаем embed URL: " + embedUrl);
        binding.vkWebView.loadUrl(embedUrl);
    }

    /**
     * Извлекает embed-URL для VK Video из предоставленной ссылки.
     * Поддерживает форматы:
     * - https://vkvideo.ru/video-21665793_456242598
     * - https://vk.com/video-21665793_456242598
     * - https://vk.com/video_ext.php?oid=-21665793&id=456242598&hd=2&autoplay=1
     * @param url Ссылка на видео VK
     * @return Embed-URL для WebView или null, если ссылка неверная
     */
    private String extractVkVideoEmbedUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            String path = uri.getPath();

            if (host.contains("vk.com") && path.startsWith("/video_ext.php")) {
                String oid = uri.getQueryParameter("oid");
                String id = uri.getQueryParameter("id");
                if (oid != null && id != null) {
                    return "https://vk.com/video_ext.php?oid=" + oid + "&id=" + id + "&hd=2&autoplay=1";
                }
            } else if (host.contains("vk.com") || host.contains("vkvideo.ru")) {
                if (path.startsWith("/video")) {
                    String videoId = path.replace("/video", "");
                    if (videoId.contains("?")) {
                        videoId = videoId.substring(0, videoId.indexOf("?"));
                    }
                    if (videoId.contains("_")) {
                        return "https://vk.com/video_ext.php?oid=" + videoId + "&hd=2&autoplay=1";
                    }
                }
            }
            Log.e("VkVideoParseError", "Неверный формат URL: " + url);
            return null;
        } catch (Exception e) {
            Log.e("VkVideoParseError", "Ошибка парсинга: " + url, e);
            return null;
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
     * Настраивает переменные и элементы интерфейса,
     * устанавливает обработчики событий для кнопок.
     */
    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.adressTxt.setText(object.getAddress());
        binding.descriptionTxt.setText(Html.fromHtml(object.getDescription(), Html.FROM_HTML_MODE_LEGACY));
        binding.bedTxt.setText(object.getBed());
        binding.ratingBar.setRating((float) object.getScore());
        l = object.getLongitude();
        w = object.getWidth();

        Glide.with(Detail_AttractionActivity.this)
                .load(object.getPic())
                .into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.favIcon.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Войдите в систему, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Detail_AttractionActivity.this, LoginActivity.class));
                return;
            }
            isFavorite = !isFavorite;
            updateFavoriteIcon();
            saveFavoriteStatus();
        });

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_AttractionActivity.this, ReviewsActivity.class);
            intent.putExtra("productId", object.getTitle());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Detail_AttractionActivity.this, MapRouteActivity.class);
            intent.putExtra("l", l);
            intent.putExtra("w", w);
            startActivity(intent);
        });
    }

    /**
     * Обновляет иконку избранного в зависимости от текущего статуса.
     */
    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.favIcon.setImageResource(R.drawable.fav);
        } else {
            binding.favIcon.setImageResource(R.drawable.fav_icon);
        }
    }

    /**
     * Сохраняет статус избранного в Firebase.
     */
    private void saveFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Attractions")
                .child(object.getId()); // Используем ID достопримечательности

        if (isFavorite) {
            favoritesRef.setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Достопримечательность добавлена в избранное"))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Ошибка сохранения избранного: ", e);
                    });
        } else {
            favoritesRef.removeValue()
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Достопримечательность удалена из избранного"))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Ошибка удаления избранного: ", e);
                    });
        }
    }

    /**
     * Загружает статус избранного из Firebase.
     */
    private void loadFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            isFavorite = false;
            updateFavoriteIcon();
            return;
        }
        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Attractions")
                .child(object.getId());

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                updateFavoriteIcon();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Ошибка загрузки статуса избранного: " + error.getMessage());
                isFavorite = false;
                updateFavoriteIcon();
            }
        });
    }

    /**
     * Получает объект достопримечательности из Intent, загружает его статус избранного.
     */
    private void getIntentExtra() {
        object = (ItemAttractions) getIntent().getSerializableExtra("object");
        loadFavoriteStatus();
    }

    /**
     * Загружает отзывы из Firebase и на их основе высчитывает рейтинг достопримечательности,
     * в соответствии с рейтингом обновляет RatingBar.
     */
    private void loadAndCalculateRating() {
        String productId = object.getTitle();

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double totalRating = 0;
                int reviewCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null && review.getProductId().equals(productId)) {
                        totalRating += review.getRating();
                        reviewCount++;
                    }
                }

                if (reviewCount > 0) {
                    double averageRating = totalRating / reviewCount;
                    binding.ratingBar.setRating((float) averageRating);
                } else {
                    binding.ratingBar.setRating(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки отзывов: " + databaseError.getMessage());
            }
        });
    }
}
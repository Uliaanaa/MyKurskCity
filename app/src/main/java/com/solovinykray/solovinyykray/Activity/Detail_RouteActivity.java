package com.solovinykray.solovinyykray.Activity;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
import com.solovinykray.solovinyykray.Domain.ItemRoute;
import com.solovinykray.solovinyykray.Domain.Review;
import com.solovinyykray.solovinyykray.R;
import com.solovinyykray.solovinyykray.databinding.ActivityDetailRouteBinding;

import java.io.IOException;
import java.util.Locale;

/**
 * Активность для отображения детальной информации о маршруте.
 * Реализует функции:
 * - Воспроизведение аудио-описания маршрута
 * - Встраивание видео из VK
 * - Управление избранными маршрутами
 * - Отображение рейтинга и отзывов
 * - Навигация на карту маршрута
 */
public class Detail_RouteActivity extends BaseActivity {
    private ActivityDetailRouteBinding binding;
    private ItemRoute object;
    private boolean isFavorite = false;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private Handler handler = new Handler();
    private SeekBar seekBar;
    private ImageButton playPauseBtn;
    private LinearLayout audioContainer;
    private ProgressBar audioProgress;
    private TextView videoTitle;
    private ValueEventListener favoriteListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        seekBar = binding.seekBar;
        playPauseBtn = binding.playPauseBtn;
        audioContainer = binding.audioContainer;
        audioProgress = binding.audioProgress;
        videoTitle = binding.videoTitle;

        getIntentExtra();
        setVariable();
        loadAndCalculateRating();
        enableImmersiveMode();
        setupAudioPlayer();
        setupVkVideoPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        if (binding.vkWebView != null) {
            binding.vkWebView.stopLoading();
            binding.vkWebView.destroy();
        }
        if (favoriteListener != null && object != null && object.getId() != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                        .getReference("Favorites")
                        .child(uid)
                        .child("Routes")
                        .child(object.getId());
                favoritesRef.removeEventListener(favoriteListener);
            }
        }
    }

    /**
     * Обновляет иконку избранного состояния на основе текущего статуса.
     * Использует разные ресурсы для отображения состояния "в избранном" и "не в избранном".
     */
    private void updateFavoriteIcon() {
        binding.favIcon.setImageResource(isFavorite ? R.drawable.fav : R.drawable.fav_icon);
    }

    /**
     * Переключает статус избранного для текущего маршрута.
     * Выполняет запись/удаление из Firebase Realtime Database.
     * Требует авторизации пользователя. При отсутствии авторизации перенаправляет на экран логина.
     */
    private void toggleFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || object.getId() == null || object.getId().isEmpty()) {
            Toast.makeText(this, "Ошибка: идентификатор маршрута отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Routes")
                .child(object.getId());

        isFavorite = !isFavorite;
        if (isFavorite) {
            favoritesRef.setValue(true);
        } else {
            favoritesRef.removeValue();
        }
        updateFavoriteIcon();
    }

    /**
     * Загружает статус избранного для текущего маршрута из Firebase.
     * Устанавливает начальное состояние иконки. Регистрирует слушатель для отслеживания изменений в реальном времени.
     */
    private void loadFavoriteStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || object.getId() == null || object.getId().isEmpty()) {
            isFavorite = false;
            updateFavoriteIcon();
            return;
        }

        String uid = user.getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance()
                .getReference("Favorites")
                .child(uid)
                .child("Routes")
                .child(object.getId());

        favoriteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                updateFavoriteIcon();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isFavorite = false;
                updateFavoriteIcon();
            }
        };
        favoritesRef.addValueEventListener(favoriteListener);
    }

    /**
     * Инициализирует аудиоплеер для воспроизведения аудио-описания маршрута.
     * Проверяет наличие валидной ссылки на аудиофайл в Firebase Storage.
     * Настраивает элементы управления: кнопку play/pause и seekbar.
     */
    private void setupAudioPlayer() {
        binding.audioContainer.setVisibility(View.GONE);

        if (object.getAudioToken() != null && !object.getAudioToken().isEmpty()) {
            if (isValidFirebaseStorageUrl(object.getAudioToken())) {
                binding.audioContainer.setVisibility(View.VISIBLE);
                initMediaPlayer(object.getAudioToken());
            }
        }

        binding.playPauseBtn.setOnClickListener(v -> {
            if (mediaPlayer != null && isPrepared) {
                if (mediaPlayer.isPlaying()) {
                    pauseAudio();
                } else {
                    playAudio();
                }
            }
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * Проверяет, является ли URL валидной ссылкой Firebase Storage.
     *
     * @param url Проверяемый URL
     * @return true если URL соответствует формату Firebase Storage, иначе false
     */
    private boolean isValidFirebaseStorageUrl(String url) {
        return url != null && url.startsWith("https://firebasestorage.googleapis.com/");
    }

    /**
     * Настраивает WebView для встраивания видео из VK.
     * Парсит исходный URL видео и преобразует в embed-формат.
     * Обрабатывает ошибки загрузки и скрывает элементы при отсутствии видео.
     */
    private void setupVkVideoPlayer() {
        String videoUrl = object.getVideoUrl();
        if (videoUrl == null || videoUrl.isEmpty()) {
            binding.vkWebView.setVisibility(View.GONE);
            binding.videoTitle.setVisibility(View.GONE);
            return;
        }

        String embedUrl = extractVkVideoEmbedUrl(videoUrl);

        if (embedUrl == null || embedUrl.isEmpty()) {
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
                binding.audioProgress.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                binding.vkWebView.setVisibility(View.GONE);
                binding.videoTitle.setVisibility(View.GONE);
            }
        });

        binding.audioProgress.setVisibility(View.VISIBLE);
        binding.vkWebView.loadUrl(embedUrl);
        pauseAudio();
    }

    /**
     * Преобразует стандартный URL видео VK в embed-формат для встраивания.
     * Поддерживает различные форматы VK-ссылок.
     *
     * @param url Оригинальный URL видео
     * @return Embed URL для встраивания или null при ошибке парсинга
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
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Инициализирует MediaPlayer для воспроизведения аудио.
     * Выполняет асинхронную подготовку плеера. Обновляет UI при готовности.
     * Обрабатывает ошибки инициализации и воспроизведения.
     *
     * @param audioUrl URL аудиофайла для воспроизведения
     */
    private void initMediaPlayer(String audioUrl) {
        binding.audioProgress.setVisibility(View.VISIBLE);
        binding.playPauseBtn.setEnabled(false);

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(audioUrl);

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                binding.audioProgress.setVisibility(View.GONE);
                binding.playPauseBtn.setEnabled(true);
                binding.seekBar.setMax(mediaPlayer.getDuration());
                updateSeekBar();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                binding.playPauseBtn.setImageResource(R.drawable.ic_play);
                binding.seekBar.setProgress(0);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                binding.audioContainer.setVisibility(View.GONE);
                return true;
            });

            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            binding.audioContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Обновляет позицию SeekBar в соответствии с прогрессом воспроизведения.
     * Форматирует и отображает текущее время трека.
     * Рекурсивно вызывает себя при активном воспроизведении.
     */
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            TextView timeText = findViewById(R.id.timeText);
            timeText.setText(formatTime(mediaPlayer.getCurrentPosition()));

            if (mediaPlayer.isPlaying()) {
                handler.postDelayed(this::updateSeekBar, 1000);
            }
        }
    }

    /**
     * Форматирует миллисекунды в читаемый вид (MM:SS).
     *
     * @param milliseconds Время в миллисекундах
     * @return Отформатированная строка времени
     */
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Начинает воспроизведение аудио.
     * Обновляет иконку кнопки на "паузу" и запускает обновление SeekBar.
     */
    private void playAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
        }
    }

    /**
     * Приостанавливает воспроизведение аудио.
     * Обновляет иконку кнопки на "воспроизведение".
     */
    private void pauseAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * Освобождает ресурсы MediaPlayer и останавливает обработчики обновления.
     * Вызывается при уничтожении активности для предотвращения утечек ресурсов.
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            handler.removeCallbacksAndMessages(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Включает иммерсивный режим для полноэкранного отображения.
     * Скрывает системную навигацию и панель состояния.
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * Инициализирует UI элементы и обработчики событий.
     * Устанавливает заголовок, рейтинг, изображение маршрута.
     * Настраивает слушатели для кнопок: назад, избранное, отзывы, карта.
     */
    private void setVariable() {
        binding.titleTxt.setText(object.getTitle());
        binding.ratingBar.setRating((float) object.getScore());

        Glide.with(this).load(object.getPic()).into(binding.pic);

        binding.backBtn.setOnClickListener(v -> finish());

        binding.favIcon.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Войдите в систему, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            toggleFavoriteStatus();
        });

        binding.allbtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsActivity.class);
            intent.putExtra("productId", object.getId());
            startActivity(intent);
        });

        binding.addToCartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("object", object);
            startActivity(intent);
        });

        binding.descriptionTxt.setText(Html.fromHtml(object.getDetail(), Html.FROM_HTML_MODE_LEGACY));
    }

    /**
     * Получает объект маршрута из интента.
     * Проверяет валидность объекта и его ID.
     * Запускает загрузку статуса избранного.
     */
    private void getIntentExtra() {
        object = (ItemRoute) getIntent().getSerializableExtra("object");
        if (object == null) {
            Toast.makeText(this, "Ошибка: данные о маршруте отсутствуют", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (object.getId() == null || object.getId().isEmpty()) {
            Toast.makeText(this, "Ошибка: идентификатор маршрута отсутствует", Toast.LENGTH_SHORT).show();
        }
        loadFavoriteStatus();
    }

    /**
     * Загружает отзывы из Firebase и вычисляет средний рейтинг маршрута.
     * Обновляет рейтинг в UI и сохраняет расчетное значение в объекте маршрута.
     */
    private void loadAndCalculateRating() {
        String productId = object.getTitle();

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
        reviewsRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", "Ошибка загрузки отзывов: " + databaseError.getMessage());
            }
        });
    }
}